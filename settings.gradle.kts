pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.neoforged.net/releases") { name = "NeoForged" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        gradlePluginPortal()
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"
    // Picks the remapping Loom for the Fabric targets (both obfuscated).
    id("dev.kikugie.loom-back-compat") version "0.4.2"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        // One project per Minecraft version and loader, built by build.<loader>.gradle.kts.
        fun target(minecraft: String, vararg loaders: String) {
            for (loader in loaders) version("$minecraft-$loader", minecraft).buildscript("build.$loader.gradle.kts")
        }
        target("1.20.1", "forge", "fabric")
        target("1.21.1", "neoforge", "fabric")
        // The code written out in src/ (every other target is commented out there).
        vcsVersion = "1.21.1-neoforge"
    }
}

rootProject.name = "CinematicInteractions"
