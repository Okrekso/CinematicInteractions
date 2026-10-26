package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.ClientBridge;
import dev.dyslexic.cinematic.client.compat.FirstPersonBodyCompat;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/** The client half of the mod, set up from {@code Cinematic} only on a client. */
public final class CinematicClient {
    private CinematicClient() {}

    public static void init(IEventBus modBus) {
        ClientBridge.useStation = Scene::begin;
        ClientBridge.vanillaUse = ClientOnly::onVanillaUse;
        modBus.addListener((ModelEvent.RegisterAdditional event) -> event.register(HammerModel.LOCATION));
        modBus.addListener((RegisterKeyMappingsEvent event) -> {
            event.register(CampfireHint.SIT);
            event.register(FreeLook.LOOK);
        });
        modBus.addListener(BrewingBottles::hideVanillaBottles);
        modBus.addListener(ShelfBooks::emptySlots);
        modBus.addListener(PropLayer::addTo);
        modBus.addListener(KneeLayer::addTo);
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerBlockEntityRenderer(BlockEntityType.BREWING_STAND,
                BrewingBottles::new));
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerBlockEntityRenderer(BlockEntityType.CHISELED_BOOKSHELF,
                ShelfLayoutBooks::new));
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Settings.SPEC);
        modBus.addListener(Settings::onReloading);
        // the settings screen (Forge 1.20.1 has no settings screen of its own to build on)
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new SettingsScreen(parent)));
        MinecraftForge.EVENT_BUS.register(Events.class);
        FirstPersonBodyCompat.register();
    }

    public static final class Events {
        private Events() {}

        /** How far a sitting player is lowered: hips just above the ground. */
        private static final double SITTING_DROP = 0.62;
        private static final Set<Integer> SEATED = new HashSet<>();

        /** Right before the frame is drawn, after this frame's ticks (where NeoForge sends its frame event). */
        @SubscribeEvent
        public static void onFrame(TickEvent.RenderTickEvent event) {
            if (event.phase == TickEvent.Phase.START) {
                Scene.frame();
            }
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            Settings.tick();
            Scene.tick();
            ClientOnlySmoke.tick();
            ServerSmoke.tick();
            FurnaceBreakSmoke.tick();
            CampfireHint.tick();
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && mc.level.getGameTime() % 40 == 0) {
                ClientFurnaces.prune(mc.level);
            }
            Showcase.tick();
        }

        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase == TickEvent.Phase.END && event.player instanceof LocalPlayer player && player == Minecraft.getInstance().player) {
                Scene.steer(player);
            }
        }

        /** Someone sitting by a campfire is drawn lower, on the ground (their legs are posed by HumanoidModelMixin). */
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
            if (!event.isCanceled() && ClientPayloads.atStation(event.getEntity().getId()) == StationKind.CAMPFIRE) {
                event.getPoseStack().pushPose();
                event.getPoseStack().translate(0, -SITTING_DROP, 0);
                SEATED.add(event.getEntity().getId());
            }
        }

        @SubscribeEvent
        public static void onRenderPlayerDone(RenderPlayerEvent.Post event) {
            if (SEATED.remove(event.getEntity().getId())) {
                event.getPoseStack().popPose();
            }
        }

        @SubscribeEvent
        public static void onMovementInput(MovementInputUpdateEvent event) {
            Scene.movementInput(event.getInput());
        }

        @SubscribeEvent
        public static void onInteractKey(InputEvent.InteractionKeyMappingTriggered event) {
            if (Scene.active()) {
                event.setCanceled(true);
                event.setSwingHand(false);
            } else if (event.isUseItem() && event.getHand() == InteractionHand.MAIN_HAND
                    && Minecraft.getInstance().hitResult instanceof BlockHitResult hit && ClientOnly.suppressUseSwing(hit)) {
                event.setSwingHand(false);
            }
        }

        @SubscribeEvent
        public static void onScroll(InputEvent.MouseScrollingEvent event) {
            if (Scene.active() && Minecraft.getInstance().screen == null) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onRenderLevel(RenderLevelStageEvent event) {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
                Picking.capture(event);
            } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
                SceneRenderer.render(event);
            }
        }

        @SubscribeEvent
        public static void onFov(ViewportEvent.ComputeFov event) {
            if (Scene.active() && event.usedConfiguredFov()) {
                event.setFOV(CameraRig.fov(event.getFOV()));
            }
        }

        /**
         * The lean's roll. In 1.20.1 the camera itself knows no roll (CameraMixin turns its own billboard vectors with it):
         * Forge rolls the view here, right after the camera was set up.
         */
        @SubscribeEvent
        public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
            float roll = CameraRig.roll();
            if (roll != 0) {
                event.setRoll(event.getRoll() + roll);
            }
        }

        @SubscribeEvent
        public static void onRenderHand(RenderHandEvent event) {
            if (Scene.active()) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onHighlight(RenderHighlightEvent.Block event) {
            if (Scene.active()) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onGuiLayer(RenderGuiOverlayEvent.Pre event) {
            if (Overlay.hides(event.getOverlay().id())) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onGui(RenderGuiEvent.Post event) {
            Overlay.render(event.getGuiGraphics());
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onScreenOpening(ScreenEvent.Opening event) {
            Screen next = event.getNewScreen();
            if (!Scene.active() && ClientOnly.onScreenOpening(next)) {
                event.setCanceled(true);
                return;
            }
            if (!Scene.active() || next instanceof SceneScreen) {
                return;
            }
            LocalPlayer player = Minecraft.getInstance().player;
            // (the lectern's window is a book screen, not a container screen, but it holds the menu all the same)
            if (next instanceof MenuAccess<?> window && player != null
                    && window.getMenu() == player.containerMenu && window.getMenu() != player.inventoryMenu) {
                switch (Scene.answer(window.getMenu())) {
                    case OURS -> { event.setCanceled(true); return; }
                    case LATE -> {
                        event.setCanceled(true);
                        player.containerMenu = player.inventoryMenu;
                        return;
                    }
                    case FOREIGN -> { Scene.yieldToWindow(); return; }
                    case UNRELATED -> {}
                }
            }
            if (next instanceof InventoryScreen) {
                if (Scene.phase != Scene.Phase.OUTRO) event.setCanceled(true);
            } else if (Scene.phase == Scene.Phase.ACTIVE && next != null && !next.isPauseScreen() && !Scene.consulting) {
                // something else took over the screen (a server menu, another mod): drop the scene at once
                Scene.drop();
            }
        }

        @SubscribeEvent
        public static void onScreenInit(ScreenEvent.Init.Post event) {
            Showcase.onScreen(event.getScreen());
            ClientOnlySmoke.onScreen(event.getScreen());
            ServerListHint.onScreenInit(event);
        }

        @SubscribeEvent
        public static void onChunkUnload(ChunkEvent.Unload event) {
            if (event.getLevel().isClientSide()) {
                ClientTables.unloadChunk(event.getChunk().getPos());
                ClientFurnaces.unloadChunk(event.getChunk().getPos());
                ClientBrewing.unloadChunk(event.getChunk().getPos());
                ClientEnchants.unloadChunk(event.getChunk().getPos());
                ClientMenus.unloadChunk(event.getChunk().getPos());
                ShelfBooks.unloadChunk(event.getChunk().getPos());
            }
        }

        /**
         * A player out of sight is forgotten by the client, along with whatever they were doing: if they stepped away
         * from a station meanwhile, nobody tells us, and they would come back still posed at it (sitting in the ground
         * by a campfire that is not there). Coming back into sight, the server says again if they are at one.
         */
        @SubscribeEvent
        public static void onEntityLeave(EntityLeaveLevelEvent event) {
            if (event.getLevel().isClientSide() && event.getEntity() instanceof Player) {
                ClientPayloads.AT_STATION.remove(event.getEntity().getId());
                ClientMenus.forgetEntity(event.getEntity().getId());
                ShelfBooks.forgetEntity(event.getEntity().getId());
            }
        }

        @SubscribeEvent
        public static void onLevelUnload(LevelEvent.Unload event) {
            if (event.getLevel().isClientSide()) {
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
        }

        @SubscribeEvent
        public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
            ClientOnly.onLogin();
        }

        @SubscribeEvent
        public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
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
}
