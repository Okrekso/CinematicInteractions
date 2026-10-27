plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.3"

stonecutter parameters {
    // Renames between game versions, applied to every source file in both directions.
    replacements {
        string(current.parsed >= "26.1") {
            replace("GuiGraphics", "GuiGraphicsExtractor")
            replace("ClickType", "ContainerInput")
            replace("handleInventoryMouseClick", "handleContainerInput")
            replace("net.minecraft.client.renderer.LightTexture", "net.minecraft.util.LightCoordsUtil")
            replace("LightTexture", "LightCoordsUtil")
            replace("getLightColor(", "getLightCoords(")
            replace("PayloadTypeRegistry.playS2C()", "PayloadTypeRegistry.clientboundPlay()")
            replace("PayloadTypeRegistry.playC2S()", "PayloadTypeRegistry.serverboundPlay()")
            replace("net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper", "net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper")
            replace("KeyBindingHelper.registerKeyBinding", "KeyMappingHelper.registerKeyMapping")
            replace("KeyBindingHelper", "KeyMappingHelper")
            replace("resizeDisplay()", "resizeGui()")
            replace("ServerEntityWorldChangeEvents", "ServerEntityLevelChangeEvents")
            replace("START_WORLD_TICK", "START_LEVEL_TICK")
            replace("END_WORLD_TICK", "END_LEVEL_TICK")
            replace("AFTER_PLAYER_CHANGE_WORLD", "AFTER_PLAYER_CHANGE_LEVEL")
            replace("renderWidget(", "extractWidgetRenderState(")
            replace("renderBackground(", "extractBackground(")
            replace("renderContents(", "extractContents(")
            replace("renderContent(", "extractContent(")
            replace("renderDefaultSprite(", "extractDefaultSprite(")
            replace("g.drawString(", "g.text(")
            replace("g.drawCenteredString(", "g.centeredText(")
            replace("g.drawWordWrap(", "g.textWithWordWrap(")
            replace("g.renderItemDecorations(", "g.itemDecorations(")
            replace("g.renderItem(", "g.item(")
            replace("g.renderOutline(", "g.outline(")
            replace("graphics.drawString(", "graphics.text(")
            replace("graphics.drawCenteredString(", "graphics.centeredText(")
            replace("graphics.drawWordWrap(", "graphics.textWithWordWrap(")
            replace("graphics.renderItemDecorations(", "graphics.itemDecorations(")
            replace("graphics.renderItem(", "graphics.item(")
            replace("graphics.renderOutline(", "graphics.outline(")
            replace("guiGraphics.drawString(", "guiGraphics.text(")
            replace("guiGraphics.drawCenteredString(", "guiGraphics.centeredText(")
            replace("guiGraphics.drawWordWrap(", "guiGraphics.textWithWordWrap(")
            replace("guiGraphics.renderItemDecorations(", "guiGraphics.itemDecorations(")
            replace("guiGraphics.renderItem(", "guiGraphics.item(")
            replace("guiGraphics.renderOutline(", "guiGraphics.outline(")
            replace("net.minecraft.client.renderer.state.CameraRenderState", "net.minecraft.client.renderer.state.level.CameraRenderState")
            replace("new ChunkPos(", "ChunkPos.containing(")
            replace("render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)", "extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick)")
            replace(".render(graphics, mouseX, mouseY, partialTick)", ".extractRenderState(graphics, mouseX, mouseY, partialTick)")
            replace("accessWidener v2 named", "accessWidener v2 official")
        }
        string(current.parsed >= "26.2") {
            replace("gameRenderer.getMainCamera()", "gameRenderer.mainCamera()")
            replace("Minecraft.getInstance().screen", "Minecraft.getInstance().gui.screen()")
            replace("mc.screen", "mc.gui.screen()")
            replace("mc.setScreen(", "mc.gui.setScreen(")
            replace("minecraft.setScreen(", "minecraft.gui.setScreen(")
            replace("minecraft.screen", "minecraft.gui.screen()")
            replace("net.minecraft.client.renderer.LevelRenderer.", "net.minecraft.util.LightCoordsUtil.")
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
