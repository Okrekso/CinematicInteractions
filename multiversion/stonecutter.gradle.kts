plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.3"

stonecutter parameters {
    // Renames between game versions, applied to every source file in both directions.
    replacements {
        string(current.parsed >= "26.2") {
            replace("gameRenderer.getMainCamera()", "gameRenderer.mainCamera()")
            replace("Minecraft.getInstance().screen", "Minecraft.getInstance().gui.screen()")
            replace("mc.screen", "mc.gui.screen()")
            replace("mc.setScreen(", "mc.gui.setScreen(")
            replace("minecraft.setScreen(", "minecraft.gui.setScreen(")
            replace("minecraft.screen", "minecraft.gui.screen()")
            replace("net.minecraft.client.renderer.LevelRenderer.getLightCoords(", "net.minecraft.util.LightCoordsUtil.getLightCoords(")
        }
        string(current.parsed >= "26.3") {
            replace("com.mojang.blaze3d.buffers.", "com.mojang.renderpearl.api.buffers.")
            replace("com.mojang.blaze3d.systems.GpuDevice", "com.mojang.renderpearl.api.device.GpuDevice")
            replace("com.mojang.blaze3d.textures.", "com.mojang.renderpearl.api.textures.")
            replace("com.mojang.blaze3d.GpuFormat", "com.mojang.renderpearl.api.GpuFormat")
            replace("com.mojang.blaze3d.pipeline.", "com.mojang.renderpearl.api.pipeline.")
            replace("com.mojang.blaze3d.platform.CompareOp", "com.mojang.renderpearl.api.pipeline.CompareOp")
            replace("com.mojang.blaze3d.vertex.VertexFormat", "com.mojang.renderpearl.api.vertex.VertexFormat")
            replace("InputConstants.Type.KEYSYM", "InputConstants.Type.KEYBOARD")
            replace("player.swinging", "player.isSwinging()")
            replace("event.scancode()", "event.keycode()")
        }
    }
}
