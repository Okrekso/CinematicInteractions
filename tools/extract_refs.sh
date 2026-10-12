#!/bin/bash
# Unpacks reference material from the Gradle build into refs/ (git-ignored):
#   refs/src/       decompiled Minecraft + NeoForge sources with Parchment names (what the mod compiles against)
#   refs/assets/    vanilla textures, models, blockstates, lang files (refs/data: recipes, loot, tags)
# Run ./gradlew build once first so build/moddev/artifacts exists.
set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
ART=build/moddev/artifacts
SOURCES=$(ls $ART/neoforge-*-sources.jar | head -1)
RESOURCES=$(ls $ART/neoforge-*-client-extra-aka-minecraft-resources.jar | head -1)
mkdir -p refs/src refs/assets
unzip -q -o "$SOURCES" -d refs/src
unzip -q -o "$RESOURCES" -d refs
echo "sources: $(find refs/src -name '*.java' | wc -l) files in refs/src"
echo "assets:  refs/assets/minecraft/textures/block/ ..."
