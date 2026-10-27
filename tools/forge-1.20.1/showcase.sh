#!/bin/bash
# Forge 1.20.1: builds the mod, runs the scripted in-game walkthrough (client/Showcase.java), prints its report and makes
# contact sheets of the screenshots. Same acts and frame names as the NeoForge project's tools/forge-1.20.1/showcase.sh
# (no crafter acts: Minecraft 1.20.1 has no crafter).
#
#   tools/forge-1.20.1/showcase.sh                 vanilla rendering, all acts
#   tools/forge-1.20.1/showcase.sh iris            with Embeddium + Oculus + a shader pack (run-showcase-iris/)
#   FROM=6 tools/forge-1.20.1/showcase.sh          start at a later act
#   ONLY=true FROM=0 tools/forge-1.20.1/showcase.sh   just that act
#   EXTRA="-PwithCloth" tools/forge-1.20.1/showcase.sh   extra Gradle properties (optional mods for the run)
#
# Output: <run dir>/screenshots/showcase_*.png, <run dir>/sheets/sheet_N.png, <run dir>/showcase.log.
# Every run directory keeps soundCategory_master:0.0 in its options.txt.
set -u
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT" || exit 1
if [ -z "${JAVA_HOME:-}" ] || [ ! -d "$JAVA_HOME" ]; then
    export JAVA_HOME="/c/Users/evgen/AppData/Local/Programs/Eclipse Adoptium/jdk-21.0.12.8-hotspot"
fi
if [ "${1:-}" = "iris" ]; then RUN=:1.20.1-forge:runShowcaseIris; DIR=${DIR:-versions/1.20.1-forge/run-showcase-iris}; else RUN=:1.20.1-forge:runShowcase; DIR=${DIR:-versions/1.20.1-forge/run-showcase}; fi
mkdir -p "$DIR"
touch "$DIR/options.txt"
if grep -q "^soundCategory_master:" "$DIR/options.txt"; then
    sed -i 's/^soundCategory_master:.*/soundCategory_master:0.0/' "$DIR/options.txt"
else
    echo "soundCategory_master:0.0" >> "$DIR/options.txt"
fi

./gradlew :1.20.1-forge:build --console=plain -q 2>&1 | grep -E "error|\.java:[0-9]+" | head -40
if [ "${PIPESTATUS[0]}" != "0" ]; then echo "BUILD FAILED"; exit 1; fi

# a fresh world every time: things left on tables would derail the script
rm -f "$DIR"/screenshots/showcase_*.png
rm -rf "$DIR"/saves/cinematic-showcase
timeout 3000 ./gradlew "$RUN" -Pfrom="${FROM:-0}" -Ponly="${ONLY:-false}" -PshowcaseDir="$ROOT/$DIR" -PshowcaseIrisDir="$ROOT/$DIR" ${EXTRA:-} --console=plain > "$DIR/showcase.log" 2>&1
echo "exit=$?"
grep -E "\[Showcase\] report|Exception|Caused by" "$DIR/showcase.log" | sed 's/^.*\[Showcase\] report //'
mkdir -p "$DIR/sheets"
rm -f "$DIR"/sheets/sheet_*.png
python tools/sheet.py "$DIR/screenshots" --all "$DIR/sheets"
