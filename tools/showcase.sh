#!/bin/bash
# Builds the mod, runs the scripted in-game walkthrough (client/Showcase.java), prints its report and makes
# contact sheets of the screenshots.
#
#   tools/showcase.sh                 vanilla rendering, all acts
#   tools/showcase.sh iris            with Sodium + Iris + Complementary (run-showcase-iris/)
#   FROM=6 tools/showcase.sh          start at a later act: 3 furnace, 4 smoker, 5 blast furnace, 6 enchanting,
#                                     a anvil, g grindstone, s smithing table, c stonecutter,
#                                     l loom, m cartography table, b brewing stand, r crafter, f campfire,
#                                     u soul campfire, k lectern, n beacon, h heights, o settings, t moving over,
#                                     y laying out, v looking round, j jukebox, q quick return,
#                                     e chiseled bookshelf, w what others see
#   ONLY=true FROM=0 tools/showcase.sh   just that act (it stops where the act hands over to the next one)

#
# Output: <run dir>/screenshots/showcase_*.png, <run dir>/sheets/sheet_N.png (9 shots each),
#         <run dir>/showcase.log (full game log). Don't move the mouse over the game window while it runs:
#         the simulated cursor would jump and clicks would miss.
set -u
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT" || exit 1
if [ -n "${JAVA_HOME:-}" ] && [ ! -d "$JAVA_HOME" ]; then
    echo "JAVA_HOME must point to your JDK 21 installation." >&2
    exit 1
fi
mkdir -p run-showcase run-showcase-iris
if [ "${1:-}" = "iris" ]; then RUN=runShowcaseIris; DIR=run-showcase-iris; else RUN=runShowcase; DIR=run-showcase; fi

./gradlew build --console=plain -q 2>&1 | grep -E "error|\.java:[0-9]+" | head -40
if [ "${PIPESTATUS[0]}" != "0" ]; then echo "BUILD FAILED"; exit 1; fi

# a fresh world every time: things left on tables would derail the script
rm -f "$DIR"/screenshots/showcase_*.png
rm -rf "$DIR"/saves/cinematic-showcase
timeout 3000 ./gradlew "$RUN" -Pfrom="${FROM:-0}" -Ponly="${ONLY:-false}" --console=plain > "$DIR/showcase.log" 2>&1
echo "exit=$?"
grep -E "\[Showcase\] report|Exception|Caused by" "$DIR/showcase.log" | sed 's/^.*\[Showcase\] report //'
mkdir -p "$DIR/sheets"
rm -f "$DIR"/sheets/sheet_*.png
python tools/sheet.py "$DIR/screenshots" --all "$DIR/sheets"
