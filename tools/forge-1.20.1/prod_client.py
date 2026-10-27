"""Starts a real (production, SRG-named) Forge 1.20.1 client from the Forge installs already in the Modrinth app's
meta folder, with the built Cinematic jar and chosen test mods in its own game directory. Nothing is downloaded.

    python tools/forge-1.20.1/prod_client.py --forge 47.4.0 --dir run-prod --mods build/libs/X.jar test-mods/Y.jar
        [--jvm=-Dcinematic.showcase=true ...] [--server 127.0.0.1:25589] [--log run-prod/showcase.log]

The run directory keeps soundCategory_master:0.0 in its options.txt. The username is "Dev" (offline).
"""
import argparse
import json
import os
import pathlib
import shutil
import subprocess
import sys
import uuid

META = pathlib.Path(os.environ["APPDATA"]) / "ModrinthApp" / "meta"
JAVA = META / "java_versions" / "zulu17.58.21-ca-jre17.0.15-win_x64" / "bin" / "java.exe"


def allowed(rules):
    if not rules:
        return True
    result = False
    for rule in rules:
        if "features" in rule:
            continue_ok = False
        else:
            continue_ok = True
        os_rule = rule.get("os")
        matches = continue_ok
        if os_rule:
            if "name" in os_rule and os_rule["name"] != "windows":
                matches = False
            if "arch" in os_rule and os_rule["arch"] not in ("x86_64", "amd64"):
                matches = False
        if matches:
            result = rule["action"] == "allow"
    return result


def maven_path(name):
    """group:artifact:version[:classifier][@ext] -> its path in a Maven-style library folder."""
    name, _, ext = name.partition("@")
    group, artifact, version, *classifier = name.split(":")
    file = f"{artifact}-{version}" + (f"-{classifier[0]}" if classifier else "") + "." + (ext or "jar")
    return pathlib.Path(*group.split("."), artifact, version, file)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--forge", default="47.4.0")
    ap.add_argument("--dir", required=True)
    ap.add_argument("--mods", nargs="*", default=[])
    # one JVM option per --jvm=VALUE: a bare "-Dname=value" after --jvm would be taken for an option of this script
    ap.add_argument("--jvm", action="append", default=[])
    ap.add_argument("--server")
    ap.add_argument("--log")
    ap.add_argument("--width", default="1600")
    ap.add_argument("--height", default="900")
    a = ap.parse_args()

    version = f"1.20.1-{a.forge}"
    vdir = META / "versions" / version
    data = json.loads((vdir / f"{version}.json").read_text(encoding="utf-8"))
    libraries = META / "libraries"
    natives = META / "natives" / version

    classpath = []
    for lib in data["libraries"]:
        # Modrinth lists the installer's own tools too (include_in_classpath false); on the module path they clash
        if not allowed(lib.get("rules")) or not lib.get("include_in_classpath", True):
            continue
        artifact = lib.get("downloads", {}).get("artifact")
        path = libraries / artifact["path"] if artifact and "path" in artifact else libraries / maven_path(lib["name"])
        if path.exists() and str(path) not in classpath:
            classpath.append(str(path))
    classpath.append(str(vdir / f"{version}.jar"))

    game = pathlib.Path(a.dir).resolve()
    (game / "mods").mkdir(parents=True, exist_ok=True)
    for old in (game / "mods").glob("*.jar"):
        old.unlink()
    for mod in a.mods:
        shutil.copy2(mod, game / "mods" / pathlib.Path(mod).name)
    options = game / "options.txt"
    lines = options.read_text(encoding="utf-8").splitlines() if options.exists() else []
    lines = [l for l in lines if not l.startswith("soundCategory_master:")] + ["soundCategory_master:0.0"]
    for key, value in (("onboardAccessibility", "false"), ("pauseOnLostFocus", "false"), ("skipMultiplayerWarning", "true"),
                       ("joinedFirstServer", "true"), ("tutorialStep", "none"), ("lang", "ru_ru"), ("narrator", "0")):
        if not any(l.startswith(key + ":") for l in lines):
            lines.append(f"{key}:{value}")
    options.write_text("\n".join(lines) + "\n", encoding="utf-8")

    subst = {
        "${natives_directory}": str(natives), "${launcher_name}": "cinematic-prod-test", "${launcher_version}": "1",
        "${classpath}": os.pathsep.join(classpath), "${library_directory}": str(libraries), "${classpath_separator}": os.pathsep,
        "${version_name}": version,
    }

    def fill(value):
        for k, v in subst.items():
            value = value.replace(k, v)
        return value

    jvm = ["-Xmx3G"]
    for arg in data["arguments"]["jvm"]:
        if isinstance(arg, str):
            jvm.append(fill(arg))
        elif allowed(arg.get("rules")):
            values = arg["value"] if isinstance(arg["value"], list) else [arg["value"]]
            jvm.extend(fill(v) for v in values)
    jvm.extend(a.jvm)

    offline = str(uuid.uuid3(uuid.NAMESPACE_DNS, "OfflinePlayer:Dev"))
    gamevals = {"${auth_player_name}": "Dev", "${version_name}": version, "${game_directory}": str(game),
                "${assets_root}": str(META / "assets"), "${assets_index_name}": data["assetIndex"]["id"], "${auth_uuid}": offline,
                "${auth_access_token}": "0", "${clientid}": "0", "${auth_xuid}": "0", "${user_type}": "legacy", "${version_type}": "release"}
    game_args = []
    for arg in data["arguments"]["game"]:
        if isinstance(arg, str):
            for k, v in gamevals.items():
                arg = arg.replace(k, v)
            game_args.append(arg)
    game_args += ["--width", a.width, "--height", a.height]
    if a.server:
        game_args += ["--quickPlayMultiplayer", a.server]

    cmd = [str(JAVA)] + jvm + [data["mainClass"]] + game_args
    log = open(a.log, "w", encoding="utf-8") if a.log else None
    proc = subprocess.run(cmd, cwd=game, stdout=log or sys.stdout, stderr=subprocess.STDOUT)
    sys.exit(proc.returncode)


if __name__ == "__main__":
    main()
