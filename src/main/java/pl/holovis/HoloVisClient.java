package pl.holovis;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import pl.holovis.gui.HoloScreen;

public class HoloVisClient implements ClientModInitializer {
    public static KeyBinding OPEN_MENU, SP_NEXT, SP_PREV, SP_TOGGLE;

    private static LivingEntity lastTarget;
    private static int lastTargetTicks;
    private static Vec3d lastPos = Vec3d.ZERO;
    private static float lastYaw;

    private static KeyBinding key(String name, int code) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(name, InputUtil.Type.KEYSYM, code, "key.categories.holovis"));
    }

    /** Click Sound: kliknięcie przycisku w dowolnym menu (np. ESC -> Options). */
    private static boolean onScreenClick(Screen screen, double mx, double my, int button) {
        HoloConfig cfg = HoloConfig.get();
        if (cfg.clickSound.enabled && button == 0) {
            for (Element el : screen.children()) {
                if (el instanceof ClickableWidget w && w.visible && w.active && w.isMouseOver(mx, my)) {
                    HoloSounds.play(cfg.clickSound);
                    break;
                }
            }
        }
        return true;
    }

    private static void onKill(LivingEntity e) {
        HoloConfig cfg = HoloConfig.get();
        if (cfg.killSound.enabled) HoloSounds.play(cfg.killSound);
        if (e instanceof PlayerEntity) {
            Identifier skin = e instanceof AbstractClientPlayerEntity pl ? pl.getSkinTextures().texture() : null;
            KillEffects.spawn(lastPos, lastYaw, skin);
        }
    }

    @Override
    public void onInitializeClient() {
        HoloConfig.load();
        HoloSounds.refresh();
        Spotify.sync();

        OPEN_MENU = key("key.holovis.open", GLFW.GLFW_KEY_RIGHT_SHIFT);
        SP_NEXT = key("key.holovis.spotify_next", GLFW.GLFW_KEY_RIGHT_BRACKET);
        SP_PREV = key("key.holovis.spotify_prev", GLFW.GLFW_KEY_LEFT_BRACKET);
        SP_TOGGLE = key("key.holovis.spotify_toggle", GLFW.GLFW_KEY_BACKSLASH);

        // /holovis (albo /kosa), /holovis ghost, /holovis ghost remove, /holovis killtest
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            for (String name : new String[]{"holovis", "kosa"}) {
                dispatcher.register(ClientCommandManager.literal(name)
                        .executes(ctx -> {
                            MinecraftClient mc = ctx.getSource().getClient();
                            mc.send(() -> mc.setScreen(new HoloScreen()));
                            return 1;
                        })
                        .then(ClientCommandManager.literal("ghost")
                                .executes(ctx -> { GhostBot.spawn(ctx.getSource().getClient()); return 1; })
                                .then(ClientCommandManager.literal("remove")
                                        .executes(ctx -> { GhostBot.remove(ctx.getSource().getClient()); return 1; })))
                        .then(ClientCommandManager.literal("killtest")
                                .executes(ctx -> { KillEffects.spawnTest(); return 1; })));
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (OPEN_MENU.wasPressed()) {
                if (mc.currentScreen == null && mc.player != null) mc.setScreen(new HoloScreen());
            }
            while (SP_NEXT.wasPressed()) Spotify.next();
            while (SP_PREV.wasPressed()) Spotify.previous();
            while (SP_TOGGLE.wasPressed()) Spotify.toggle();

            TargetHud.tick(mc);
            GhostBot.tick(mc);
            Cooldowns.tick(mc);
            DamageNumbers.tick();

            if (lastTarget != null) {
                lastTargetTicks++;
                if (lastTarget.isAlive()) {
                    lastPos = lastTarget.getPos();
                    lastYaw = lastTarget.getYaw();
                    if (lastTargetTicks > 100) lastTarget = null;
                } else {
                    onKill(lastTarget);
                    lastTarget = null;
                }
            }
        });

        // Uderzenie prawdziwego moba / gracza
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient && entity instanceof LivingEntity living && !GhostBot.isBot(entity)) {
                HoloConfig cfg = HoloConfig.get();
                if (cfg.hitSound.enabled) HoloSounds.play(cfg.hitSound);
                TargetHud.notifyAttack(living);
                DamageNumbers.onAttack(living);
                lastTarget = living;
                lastTargetTicks = 0;
                lastPos = living.getPos();
                lastYaw = living.getYaw();
            }
            return ActionResult.PASS;
        });

        // Atak na TestBota (anulowany, żeby nic nie poszło na serwer) + Click Sound przy machnięciu w powietrze
        ClientPreAttackCallback.EVENT.register((client, player, clickCount) -> {
            if (client.currentScreen != null || clickCount <= 0) return false;
            HitResult hr = client.crosshairTarget;
            if (hr instanceof EntityHitResult ehr && GhostBot.isBot(ehr.getEntity())) {
                GhostBot.onHit(client, player);
                return true;
            }
            HoloConfig cfg = HoloConfig.get();
            if (cfg.clickSound.enabled && (hr == null || hr.getType() == HitResult.Type.MISS)) {
                HoloSounds.play(cfg.clickSound);
            }
            return false;
        });

        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            ScreenMouseEvents.allowMouseClick(screen).register(HoloVisClient::onScreenClick);
            if (screen instanceof ChatScreen) ScreenEvents.afterRender(screen).register(SpotifyOverlay::render);
        });

        WorldRenderEvents.AFTER_ENTITIES.register(HitboxRenderer::render);
        WorldRenderEvents.AFTER_ENTITIES.register(KillEffects::render);
        WorldRenderEvents.AFTER_ENTITIES.register(TargetEsp::render);
        WorldRenderEvents.AFTER_ENTITIES.register(DamageNumbers::render);
        HudRenderCallback.EVENT.register(HudRenderer::render);
    }
}
