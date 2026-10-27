pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        gradlePluginPortal()
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"
    // Picks the remapping Loom for obfuscated versions and the plain one for 26.1+.
    id("dev.kikugie.loom-back-compat") version "0.4.2"
}

stonecutter {
    create(rootProject) {
        versions("26.1.1", "26.1.2", "26.2", "26.3")
        // The version whose code is written out in src/ (every other one is commented out there).
        vcsVersion = "26.3"
    }
}

rootProject.name = "cinematic-multiversion"
