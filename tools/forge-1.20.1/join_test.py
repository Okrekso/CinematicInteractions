"""Network test against a real dedicated Forge 1.20.1 server with Cinematic Interactions (the joinServer run).

    python tools/forge-1.20.1/join_test.py [--client dev|prod] [--forge 47.4.0] [--extra -PwithShaders ...]

1. starts the server (127.0.0.1:25589, RCON 25590), places a table, a furnace and a brewing stand;
2. a client joins and runs client/ServerSmoke: shift-clicks into the stations over the encoded connection;
3. the server is saved and stopped; the table's planks must be in world/data/cinematic_tables.dat;
4. the server starts again and a second client checks that the planks came back (ServerSmoke reconnect mode).
Only processes this script starts are stopped. Every client directory keeps soundCategory_master:0.0.
"""
import argparse
import gzip
import os
import pathlib
import socket
import struct
import subprocess
import sys
import time

REPO = pathlib.Path(__file__).resolve().parents[2]
ROOT = REPO / "versions" / "1.20.1-forge"
SERVER_DIR = ROOT / "run-join-server"
CLIENT_DIR = ROOT / "run-join-client"
GRADLE = str(REPO / "gradlew.bat")
JDK = r"C:\Users\evgen\AppData\Local\Programs\Eclipse Adoptium\jdk-21.0.12.8-hotspot"
RCON_PORT, RCON_PASSWORD = 25590, "cinematic-join-test"


def rcon(*commands):
    def packet(sock, rid, kind, body):
        data = struct.pack("<ii", rid, kind) + body.encode("utf-8") + b"\x00\x00"
        sock.sendall(struct.pack("<i", len(data)) + data)
        size = struct.unpack("<i", sock.recv(4))[0]
        rest = b""
        while len(rest) < size:
            rest += sock.recv(size - len(rest))
        return rest[8:-2].decode("utf-8", "replace")
    out = []
    with socket.create_connection(("127.0.0.1", RCON_PORT), timeout=15) as s:
        packet(s, 1, 3, RCON_PASSWORD)
        for i, c in enumerate(commands):
            reply = packet(s, 10 + i, 2, c)
            print("  rcon", c, "->", reply.strip()[:120])
            out.append(reply)
    return out


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


def gradle(args, log):
    env = dict(os.environ, JAVA_HOME=JDK)
    return subprocess.Popen([GRADLE] + args + ["--console=plain"], cwd=REPO, env=env, stdout=open(log, "w", encoding="utf-8"),
                            stderr=subprocess.STDOUT)


def wait_for(path, text, timeout, proc=None):
    end = time.time() + timeout
    while time.time() < end:
        if path.exists() and text in path.read_text(encoding="utf-8", errors="replace"):
            return True
        if proc is not None and proc.poll() is not None:
            return False
        time.sleep(2)
    return False


def start_server(extra, tag):
    log = SERVER_DIR / f"server-{tag}.log"
    proc = gradle([":1.20.1-forge:runJoinServer", "-PjoinTest"] + extra, log)
    if not wait_for(log, "Done (", 600, proc):
        print("SERVER DID NOT START", log)
        proc.kill()
        sys.exit(1)
    return proc, log


def stop_server(proc):
    try:
        rcon("save-all flush", "stop")
    except OSError as e:
        print("  rcon stop failed:", e)
    try:
        proc.wait(timeout=180)
    except subprocess.TimeoutExpired:
        proc.kill()


def finish(proc, timeout):
    """Waits for a client; a stuck one is stopped with its whole process tree (the game outlives a killed wrapper)."""
    try:
        proc.wait(timeout=timeout)
    except subprocess.TimeoutExpired:
        print(f"[join_test] client {proc.pid} timed out: stopping its process tree")
        subprocess.run(["taskkill", "/T", "/F", "/PID", str(proc.pid)], capture_output=True)


def run_client(args, extra, tag, prod, forge):
    quiet_options(CLIENT_DIR)
    log = CLIENT_DIR / f"client-{tag}.log"
    log.unlink(missing_ok=True)  # a stale log from an earlier run must not pass for this one
    if prod:
        jar = next((ROOT / "build" / "libs").glob("*.jar"))
        jvm = ["-Dcinematic.serverSmoke=true"] + (["-Dcinematic.serverSmoke.reconnect=true"] if "-PserverSmokeReconnect" in args else [])
        proc = subprocess.Popen([sys.executable, "-I", str(pathlib.Path(__file__).resolve().parent / "prod_client.py"), "--forge", forge, "--dir", str(CLIENT_DIR),
                                 "--mods", str(jar), "--server", "127.0.0.1:25589", "--log", str(log)] + [f"--jvm={o}" for o in jvm], cwd=ROOT)
    else:
        # a jar left by a production run would load beside the development classes (twice, with SRG names)
        for jar in (CLIENT_DIR / "mods").glob("*.jar"):
            jar.unlink()
        proc = gradle([":1.20.1-forge:runJoinClient", "-PjoinTest"] + args + extra, log)
    return proc, log


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--client", choices=["dev", "prod"], default="dev")
    ap.add_argument("--forge", default="47.4.0")
    ap.add_argument("--extra", nargs="*", default=[])
    a = ap.parse_args()
    prod = a.client == "prod"
    results = []

    world = SERVER_DIR / "world"
    if world.exists():
        import shutil
        shutil.rmtree(world)
    server, slog = start_server(a.extra, "1")
    rcon("setblock 0 -60 0 minecraft:crafting_table", "setblock 2 -60 0 minecraft:furnace[facing=south]",
         "setblock -2 -60 0 minecraft:brewing_stand", "gamerule doDaylightCycle false", "time set 4000")
    client, clog = run_client(["-PserverSmoke"], a.extra, "1", prod, a.forge)
    if wait_for(slog, "Dev joined the game", 600, client) or wait_for(slog, "Dev присоединился", 5):
        time.sleep(4)
        rcon("clear Dev", "item replace entity Dev inventory.0 with minecraft:oak_planks 16",
             "item replace entity Dev inventory.1 with minecraft:raw_iron 5", "item replace entity Dev inventory.2 with minecraft:dirt 3",
             'item replace entity Dev inventory.3 with minecraft:potion{Potion:"minecraft:water"} 1', "tp Dev 0.5 -60 3.5 180 20")
    finish(client, 900)
    text = clog.read_text(encoding="utf-8", errors="replace")
    smoke = [l for l in text.splitlines() if "[ServerSmoke]" in l]
    results.extend(smoke)
    passed = any("PASS" in l for l in smoke)
    stop_server(server)
    data = world / "data" / "cinematic_tables.dat"
    saved = data.exists() and b"oak_planks" in gzip.decompress(data.read_bytes())
    results.append(f"[join_test] world save: {data.name} {'holds' if saved else 'LACKS'} the table's planks")

    if passed and saved:
        server, slog = start_server(a.extra, "2")
        client, clog = run_client(["-PserverSmokeReconnect"], a.extra, "2", prod, a.forge)
        finish(client, 600)
        results.extend(l for l in clog.read_text(encoding="utf-8", errors="replace").splitlines() if "[ServerSmoke]" in l)
        stop_server(server)
    print("\n".join(results))


if __name__ == "__main__":
    main()
