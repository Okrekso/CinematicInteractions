"""Client-only mode against a clean VANILLA 1.20.1 server (no Forge, no Cinematic Interactions).

    python tools/forge-1.20.1/client_only_test.py [--client dev|prod] [--forge 47.4.0] [--server vanilla|forge] [--extra -PwithJei ...]

The server is the vanilla server jar Gradle already keeps for this Minecraft version (run-vanilla-server/server.jar,
127.0.0.1:25586, RCON 25591). It builds a platform at y 63 with a table (0,64,1), a furnace (1,64,1) and an anvil
(1,64,3), puts the player Dev at (0.9, 64, 2.7) with an oak log in the first slot and some raw iron and dirt, and the
client runs client/ClientOnlySmoke: [ClientOnlySmoke] PASS or FAIL in its log. Only processes started here are stopped.
--server forge uses a Forge 1.20.1 server WITHOUT Cinematic Interactions instead (the plainServer run, run-plain-server/,
same ports): Forge's handshake must let the client in, and the client must fall back to client-only mode.
"""
import argparse
import os
import pathlib
import socket
import struct
import subprocess
import sys
import time

REPO = pathlib.Path(__file__).resolve().parents[2]
ROOT = REPO / "versions" / "1.20.1-forge"
SERVER = ROOT / "run-vanilla-server"
CLIENT = ROOT / "run-client-only"
JAVA17 = pathlib.Path(os.environ["APPDATA"]) / "ModrinthApp" / "meta" / "java_versions" / "zulu17.58.21-ca-jre17.0.15-win_x64" / "bin" / "java.exe"
JDK = r"C:\Users\evgen\AppData\Local\Programs\Eclipse Adoptium\jdk-21.0.12.8-hotspot"


def rcon(*commands):
    def packet(sock, rid, kind, body):
        data = struct.pack("<ii", rid, kind) + body.encode("utf-8") + b"\x00\x00"
        sock.sendall(struct.pack("<i", len(data)) + data)
        size = struct.unpack("<i", sock.recv(4))[0]
        rest = b""
        while len(rest) < size:
            rest += sock.recv(size - len(rest))
        return rest[8:-2].decode("utf-8", "replace")
    with socket.create_connection(("127.0.0.1", 25591), timeout=15) as s:
        packet(s, 1, 3, "cinematic-smoke")
        for i, c in enumerate(commands):
            print("  rcon", c, "->", packet(s, 10 + i, 2, c).strip()[:120])


def wait_for(path, text, timeout, proc=None):
    end = time.time() + timeout
    while time.time() < end:
        if path.exists() and text in path.read_text(encoding="utf-8", errors="replace"):
            return True
        if proc is not None and proc.poll() is not None:
            return False
        time.sleep(2)
    return False


def quiet_options(directory):
    directory.mkdir(parents=True, exist_ok=True)
    options = directory / "options.txt"
    lines = options.read_text(encoding="utf-8").splitlines() if options.exists() else []
    lines = [l for l in lines if not l.startswith("soundCategory_master:")] + ["soundCategory_master:0.0"]
    for key, value in (("onboardAccessibility", "false"), ("pauseOnLostFocus", "false"), ("skipMultiplayerWarning", "true"),
                       ("joinedFirstServer", "true"), ("tutorialStep", "none"), ("lang", "ru_ru")):
        if not any(l.startswith(key + ":") for l in lines):
            lines.append(f"{key}:{value}")
    options.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--client", choices=["dev", "prod"], default="dev")
    ap.add_argument("--forge", default="47.4.0")
    ap.add_argument("--server", choices=["vanilla", "forge"], default="vanilla")
    ap.add_argument("--extra", nargs="*", default=[])
    a = ap.parse_args()

    import shutil
    folder = SERVER if a.server == "vanilla" else ROOT / "run-plain-server"
    if a.server == "forge":
        folder.mkdir(exist_ok=True)
        shutil.copy(SERVER / "server.properties", folder / "server.properties")
        shutil.copy(SERVER / "eula.txt", folder / "eula.txt")
    if (folder / "world").exists():
        shutil.rmtree(folder / "world")
    slog = folder / "server.log"
    if a.server == "vanilla":
        server = subprocess.Popen([str(JAVA17), "-Xmx1G", "-jar", "server.jar", "nogui"], cwd=SERVER, stdin=subprocess.PIPE,
                                  stdout=open(slog, "w", encoding="utf-8"), stderr=subprocess.STDOUT)
    else:
        server = subprocess.Popen([str(REPO / "gradlew.bat"), ":1.20.1-forge:runPlainServer", "-PjoinTest", "--console=plain"], cwd=REPO,
                                  env=dict(os.environ, JAVA_HOME=JDK), stdin=subprocess.PIPE,
                                  stdout=open(slog, "w", encoding="utf-8"), stderr=subprocess.STDOUT)
    if not wait_for(slog, "Done (", 300, server):
        print("SERVER DID NOT START")
        server.kill()
        sys.exit(1)
    rcon("fill -4 63 -2 5 63 7 minecraft:stone", "fill -4 64 -2 5 67 7 minecraft:air", "setblock 0 64 1 minecraft:crafting_table",
         "setblock 1 64 1 minecraft:furnace[facing=south]", "setblock 1 64 3 minecraft:anvil[facing=north]",
         "setworldspawn 0 64 3", "gamerule doDaylightCycle false", "time set 4000", "gamerule doMobSpawning false")

    quiet_options(CLIENT)
    clog = CLIENT / "client.log"
    clog.unlink(missing_ok=True)  # a stale log from an earlier run must not pass for this one
    if a.client == "prod":
        jar = next((ROOT / "build" / "libs").glob("*.jar"))
        client = subprocess.Popen([sys.executable, "-I", str(pathlib.Path(__file__).resolve().parent / "prod_client.py"), "--forge", a.forge, "--dir", str(CLIENT),
                                   "--mods", str(jar), "--log", str(clog), "--width", "1280", "--height", "720",
                                   "--jvm=-Dcinematic.clientOnlySmoke=true"], cwd=REPO)
    else:
        # a jar left by a production run would load beside the development classes (twice, with SRG names)
        for jar in (CLIENT / "mods").glob("*.jar"):
            jar.unlink()
        env = dict(os.environ, JAVA_HOME=JDK)
        client = subprocess.Popen([str(REPO / "gradlew.bat"), ":1.20.1-forge:runClientOnlySmoke", f"-PclientOnlySmokeDir={CLIENT.name}", "--console=plain"]
                                  + a.extra, cwd=REPO, env=env, stdout=open(clog, "w", encoding="utf-8"), stderr=subprocess.STDOUT)
    if wait_for(slog, "Dev joined the game", 600, client) or wait_for(slog, "Dev присоединился", 1, client):
        time.sleep(3)
        rcon("clear Dev", "item replace entity Dev hotbar.0 with minecraft:oak_log 1", "item replace entity Dev inventory.0 with minecraft:raw_iron 3",
             "item replace entity Dev inventory.1 with minecraft:dirt 2", "tp Dev 0.9 64 2.7 180 30")
    try:
        client.wait(timeout=900)
    except subprocess.TimeoutExpired:
        # the game outlives a killed wrapper: stop the whole tree this script started
        subprocess.run(["taskkill", "/T", "/F", "/PID", str(client.pid)], capture_output=True)
    rcon("stop")
    try:
        server.wait(timeout=120)
    except subprocess.TimeoutExpired:
        server.kill()
    lines = [l for l in clog.read_text(encoding="utf-8", errors="replace").splitlines() if "[ClientOnlySmoke]" in l]
    print("\n".join(lines))
    # the project folder is called "Cinematic Interactions": only a mention outside the paths counts
    text = slog.read_text(encoding="utf-8", errors="replace").replace("Cinematic Interactions", "").replace("Cinematic%20Interactions", "")
    print("[client_only_test] server:", a.server, "| server log mentions cinematic:", "cinematic" in text.lower())


if __name__ == "__main__":
    main()
