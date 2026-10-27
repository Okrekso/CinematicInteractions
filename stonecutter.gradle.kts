plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.21.1-neoforge"

stonecutter parameters {
    val (version, loader) = current.project.split('-', limit = 2)

    // Loads the [<loader>."<version>"] tables of stonecutter.properties.toml.
    properties {
        tags(version, loader)
    }

    // Loader constants for the comments: //? if fabric {, //? if !neoforge {, ...
    constants {
        match(loader, "fabric", "neoforge", "forge")
    }

    // Renames between game versions, applied to every source file in both directions.
    replacements {
        string(current.parsed >= "1.21") {
            replace("ItemStack.isSameItemSameTags(", "ItemStack.isSameItemSameComponents(")
            replace("dev.dyslexic.cinematic.Compat.hashItem(", "ItemStack.hashItemAndComponents(")
            replace("mc.getFrameTime()", "mc.getTimer().getGameTimeDeltaPartialTick(false)")
            replace("SoundEvents.ARMOR_EQUIP_LEATHER", "SoundEvents.ARMOR_EQUIP_LEATHER.value()")
            replace(".vertex(", ".addVertex(")
            replace(".uv(", ".setUv(")
        }
    }
}
