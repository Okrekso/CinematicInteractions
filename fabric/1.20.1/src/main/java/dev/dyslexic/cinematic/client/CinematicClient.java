package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.ClientBridge;
import dev.dyslexic.cinematic.client.compat.FirstPersonBodyCompat;
import dev.dyslexic.cinematic.net.AtStationPayload;
import dev.dyslexic.cinematic.net.BrewingContentsPayload;
import dev.dyslexic.cinematic.net.CampfireProgressPayload;
import dev.dyslexic.cinematic.net.EnchantDisplayPayload;
import dev.dyslexic.cinematic.net.EnchantFxPayload;
import dev.dyslexic.cinematic.net.FurnaceContentsPayload;
import dev.dyslexic.cinematic.net.FurnaceStatePayload;
import dev.dyslexic.cinematic.net.MenuDisplayPayload;
import dev.dyslexic.cinematic.net.MenuFxPayload;
import dev.dyslexic.cinematic.net.ProtocolPayload;
import dev.dyslexic.cinematic.net.SessionClosePayload;
import dev.dyslexic.cinematic.net.TableContentsPayload;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.HashSet;
import java.util.Set;
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
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * The client side, as NeoForge's CinematicClient: the events it listens to are Fabric API callbacks here, and where
 * Fabric API has none, a mixin calls the same handler from the same place in the game (named on each method).
 */
public final class CinematicClient implements ClientModInitializer {
    /** How far a sitting player is lowered: hips just above the ground. */
    private static final double SITTING_DROP = 0.62;
    private static final Set<Integer> SEATED = new HashSet<>();

    @Override
    public void onInitializeClient() {
        ClientBridge.useStation = Scene::begin;
        ClientBridge.vanillaUse = ClientOnly::onVanillaUse;
        Settings.SPEC.load();
        FirstPersonBodyCompat.register();
        KeyBindingHelper.registerKeyBinding(CampfireHint.SIT);
        KeyBindingHelper.registerKeyBinding(FreeLook.LOOK);
        ModelLoadingPlugin.register(context -> {
            context.addModels(HammerModel.LOCATION);
            context.modifyModelAfterBake().register((model, ctx) -> StationModels.wrap(model, ctx.id()));
        });
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
            if (renderer instanceof PlayerRenderer player) {
                helper.register(new PropLayer(player));
                helper.register(new KneeLayer(player));
            }
        });
        BlockEntityRendererRegistry.register(BlockEntityType.BREWING_STAND, BrewingBottles::new);
        BlockEntityRendererRegistry.register(BlockEntityType.CHISELED_BOOKSHELF, ShelfLayoutBooks::new);

        ClientPlayNetworking.registerGlobalReceiver(ProtocolPayload.TYPE, (p, player, sender) -> {});
        ClientPlayNetworking.registerGlobalReceiver(TableContentsPayload.TYPE, (p, player, sender) -> ClientPayloads.tableContents(p));
        ClientPlayNetworking.registerGlobalReceiver(FurnaceContentsPayload.TYPE, (p, player, sender) -> ClientPayloads.furnaceContents(p));
        ClientPlayNetworking.registerGlobalReceiver(BrewingContentsPayload.TYPE, (p, player, sender) -> ClientBrewing.apply(p));
        ClientPlayNetworking.registerGlobalReceiver(FurnaceStatePayload.TYPE, (p, player, sender) -> ClientPayloads.furnaceState(p));
        ClientPlayNetworking.registerGlobalReceiver(CampfireProgressPayload.TYPE, (p, player, sender) -> ClientPayloads.campfireProgress(p));
        ClientPlayNetworking.registerGlobalReceiver(EnchantDisplayPayload.TYPE, (p, player, sender) -> ClientPayloads.enchantDisplay(p));
        ClientPlayNetworking.registerGlobalReceiver(EnchantFxPayload.TYPE, (p, player, sender) -> ClientPayloads.enchantFx(p));
        ClientPlayNetworking.registerGlobalReceiver(MenuDisplayPayload.TYPE, (p, player, sender) -> ClientPayloads.menuDisplay(p));
        ClientPlayNetworking.registerGlobalReceiver(MenuFxPayload.TYPE, (p, player, sender) -> ClientPayloads.menuFx(p));
        ClientPlayNetworking.registerGlobalReceiver(SessionClosePayload.TYPE, (p, player, sender) -> ClientPayloads.close(p));
        ClientPlayNetworking.registerGlobalReceiver(AtStationPayload.TYPE, (p, player, sender) -> ClientPayloads.atStation(p));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            Scene.tick();
            ClientOnlySmoke.tick();
            ServerSmoke.tick();
            ServerHintSmoke.tick();
            CampfireHint.tick();
            ClientPayloads.faceStations(mc.level);
            if (mc.level != null && mc.level.getGameTime() % 40 == 0) {
                ClientFurnaces.prune(mc.level);
            }
            Showcase.tick();
        });
        // NeoForge's RenderLevelStageEvent: the matrices of this frame, and the scene drawn after the entities
        WorldRenderEvents.START.register(Picking::capture);
        WorldRenderEvents.AFTER_ENTITIES.register(SceneRenderer::render);
        // RenderHighlightEvent.Block: no block selection box over a scene
        WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register((context, hit) -> !Scene.active());
        HudRenderCallback.EVENT.register((graphics, tick) -> Overlay.render(graphics));
        ScreenEvents.AFTER_INIT.register((mc, screen, width, height) -> {
            ServerListHint.onScreenInit(mc, screen);
            Showcase.onScreen(screen);
            ClientOnlySmoke.onScreen(screen);
            ServerHintSmoke.onScreen(screen);
        });
        ClientChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> {
            ClientTables.unloadChunk(chunk.getPos());
            ClientFurnaces.unloadChunk(chunk.getPos());
            ClientBrewing.unloadChunk(chunk.getPos());
            ClientEnchants.unloadChunk(chunk.getPos());
            ClientMenus.unloadChunk(chunk.getPos());
            ShelfBooks.unloadChunk(chunk.getPos());
        });
        // A player out of sight is forgotten by the client, along with whatever they were doing: if they stepped away
        // from a station meanwhile, nobody tells us, and they would come back still posed at it (sitting in the ground
        // by a campfire that is not there). Coming back into sight, the server says again if they are at one.
        ClientEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            if (entity instanceof Player) {
                ClientPayloads.AT_STATION.remove(entity.getId());
                ClientMenus.forgetEntity(entity.getId());
                ShelfBooks.forgetEntity(entity.getId());
            }
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, mc) -> ClientOnly.onLogin());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> onLogout());
    }

    /**
     * The settings screen (NeoForge registers it as the mod's config screen; on Fabric, Mod Menu opens it).
     */
    public static Screen settingsScreen(Screen parent) {
        return new SettingsScreen(parent);
    }

    /** NeoForge's PlayerTickEvent.Post for the local player (LocalPlayerMixin, right after Player.tick). */
    public static void onPlayerTick(LocalPlayer player) {
        if (player == Minecraft.getInstance().player) {
            Scene.steer(player);
        }
    }

    /** Someone sitting by a campfire is drawn lower, on the ground (their legs are posed by HumanoidModelMixin). */
    public static boolean seat(Player player) {
        if (ClientPayloads.atStation(player.getId()) == StationKind.CAMPFIRE) {
            SEATED.add(player.getId());
            return true;
        }
        return false;
    }

    /** True when {@link #seat} lowered this player and the pose has to be put back. */
    public static boolean unseat(Player player) {
        return SEATED.remove(player.getId());
    }

    public static double sittingDrop() {
        return SITTING_DROP;
    }

    /**
     * NeoForge's ScreenEvent.Opening (MinecraftMixin, at the start of setScreen; as there, only for a screen opening, not
     * for one closing). True cancels the new screen.
     */
    public static boolean onScreenOpening(Screen next) {
        if (!Scene.active() && ClientOnly.onScreenOpening(next)) {
            return true;
        }
        if (!Scene.active() || next instanceof SceneScreen) {
            return false;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        // A menu the server has just opened (MenuScreens set it as the player's menu before the screen). Never the
        // player's own inventory: once stepping away, a station's menu has gone back to it, and the inventory opened
        // then is no menu of the block's.
        // (the lectern's window is a book screen, not a container screen, but it holds the menu all the same)
        if (next instanceof MenuAccess<?> window && player != null && window.getMenu() != player.inventoryMenu
                && window.getMenu() == player.containerMenu) {
            switch (Scene.answer(window.getMenu())) {
                case OURS -> {
                    // the server opened the block's menu for us: keep the menu, the scene stands in for its window
                    return true;
                }
                case LATE -> {
                    // the answer to a BEGIN of a station already let go of; the server closes that menu on the LEAVE
                    // it got after it, so no window opens and the menu goes quietly here too
                    player.containerMenu = player.inventoryMenu;
                    return true;
                }
                case FOREIGN -> {
                    // the block opened a menu the scene does not know (a mod replaced it, as Penchant does the
                    // enchanting table's on NeoForge): its own window must open, or the window built for it is left
                    // half-made and the mod's menu calls into it on the next slot change
                    Scene.yieldToWindow();
                    return false;
                }
                case UNRELATED -> {}
            }
        }
        if (next instanceof InventoryScreen) {
            // while walking up and working, E steps away instead; once stepping away, the inventory opens over it and
            // the step away goes on behind it
            return Scene.phase != Scene.Phase.OUTRO;
        }
        if (Scene.phase == Scene.Phase.ACTIVE && !next.isPauseScreen() && !Scene.consulting) {
            // something else took over the screen (a server menu, another mod): drop the scene at once
            Scene.drop();
        }
        return false;
    }

    /** NeoForge's LevelEvent.Unload on the client (MinecraftMixin: a new level replaces it, or the player leaves it). */
    public static void onLevelUnload() {
        Scene.drop();
        ClientTables.clear();
        ClientFurnaces.clear();
        ClientBrewing.clear();
        ClientEnchants.clear();
        ClientMenus.clear();
        ShelfBooks.clear();
        Fx.clearSparks();
        ClientPayloads.clear();
    }

    private static void onLogout() {
        Scene.drop();
        Scene.forgetOwnWindows();
        ClientOnly.clear();
        ClientTables.clear();
        ClientFurnaces.clear();
        ClientBrewing.clear();
        ClientEnchants.clear();
        ClientMenus.clear();
        ShelfBooks.clear();
        ClientPayloads.clear();
    }
}
