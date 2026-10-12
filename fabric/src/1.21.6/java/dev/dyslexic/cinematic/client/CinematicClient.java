package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.ClientBridge;
import dev.dyslexic.cinematic.net.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class CinematicClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CinematicPipelines.initialize();
        ClientBridge.useStation = Scene::begin;
        Settings.SPEC.load();
        KeyBindingHelper.registerKeyBinding(CampfireHint.SIT);
        KeyBindingHelper.registerKeyBinding(FreeLook.LOOK);
        ModelLoadingPlugin.register(context -> {
            context.modifyBlockModelAfterBake().register((model, ctx) -> StationModels.wrap(model, ctx.state()));
        });
        BlockEntityRendererRegistry.register(BlockEntityType.BREWING_STAND, BrewingBottles::new);
        net.fabricmc.fabric.api.client.rendering.v1.SpecialGuiElementRegistry.register(context -> new LoomFlags.Renderer(context.vertexConsumers()));
        ClientPlayNetworking.registerGlobalReceiver(RecipeSyncPayload.TYPE, (p, c) -> ClientRecipes.accept(p));
        ClientPlayNetworking.registerGlobalReceiver(TableContentsPayload.TYPE, (p, c) -> ClientPayloads.tableContents(p));
        ClientPlayNetworking.registerGlobalReceiver(FurnaceContentsPayload.TYPE, (p, c) -> ClientPayloads.furnaceContents(p));
        ClientPlayNetworking.registerGlobalReceiver(FurnaceStatePayload.TYPE, (p, c) -> ClientPayloads.furnaceState(p));
        ClientPlayNetworking.registerGlobalReceiver(EnchantDisplayPayload.TYPE, (p, c) -> ClientPayloads.enchantDisplay(p));
        ClientPlayNetworking.registerGlobalReceiver(EnchantFxPayload.TYPE, (p, c) -> ClientPayloads.enchantFx(p));
        ClientPlayNetworking.registerGlobalReceiver(MenuDisplayPayload.TYPE, (p, c) -> ClientPayloads.menuDisplay(p));
        ClientPlayNetworking.registerGlobalReceiver(MenuFxPayload.TYPE, (p, c) -> ClientPayloads.menuFx(p));
        ClientPlayNetworking.registerGlobalReceiver(SessionClosePayload.TYPE, (p, c) -> ClientPayloads.close(p));
        ClientPlayNetworking.registerGlobalReceiver(AtStationPayload.TYPE, (p, c) -> ClientPayloads.atStation(p));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            Scene.tick();
            CampfireHint.tick();
            if (mc.level != null && mc.level.getGameTime() % 40 == 0) ClientFurnaces.prune(mc.level);
            Showcase.tick();
        });
        WorldRenderEvents.START.register(Picking::capture);
        WorldRenderEvents.AFTER_ENTITIES.register(SceneRenderer::render);
        // as NeoForge's RenderHighlightEvent.Block: no block selection box over a scene
        WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register((context, hit) -> !Scene.active());
        HudRenderCallback.EVENT.register((g, ticks) -> Overlay.render(g));
        ClientChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> {
            ClientTables.unloadChunk(chunk.getPos());
            ClientFurnaces.unloadChunk(chunk.getPos());
            ClientEnchants.unloadChunk(chunk.getPos());
            ClientMenus.unloadChunk(chunk.getPos());
            ShelfBooks.unloadChunk(chunk.getPos());
        });
        ClientEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity instanceof Player) {
                ClientPayloads.AT_STATION.remove(entity.getId());
                ClientMenus.forgetEntity(entity.getId());
                ShelfBooks.forgetEntity(entity.getId());
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> clear());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, mc) -> {
            if (!ClientPlayNetworking.canSend(StationActionPayload.TYPE)) {
                handler.getConnection().disconnect(Component.literal("Cinematic Interactions must be installed on both the client and server."));
            }
        });
    }

    public static boolean screenOpening(Screen next) {
        if (!Scene.active() || next instanceof SceneScreen) return false;
        var player = Minecraft.getInstance().player;
        if (next instanceof MenuAccess<?> window && Scene.station.kind().usesMenu() && player != null && window.getMenu() == player.containerMenu) return true;
        if (next instanceof InventoryScreen) return true;
        if (Scene.phase == Scene.Phase.ACTIVE && next != null && !next.isPauseScreen()) Scene.drop();
        return false;
    }

    public static void clear() {
        Scene.drop();
        ClientRecipes.clear();
        ClientTables.clear();
        ClientFurnaces.clear();
        ClientEnchants.clear();
        ClientMenus.clear();
        ShelfBooks.clear();
        Fx.clearSparks();
        ClientPayloads.clear();
    }
}
