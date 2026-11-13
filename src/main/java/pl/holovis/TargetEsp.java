package pl.holovis;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Util;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** Target ESP: świecący pierścień krążący w górę i w dół wokół celu (albo okrąg pod jego nogami). */
public final class TargetEsp {
    private TargetEsp() {}

    public static void render(WorldRenderContext ctx) {
        HoloConfig cfg = HoloConfig.get();
        if (!cfg.targetEsp) return;
        LivingEntity t = TargetHud.currentTarget();
        if (t == null) return;
        MatrixStack ms = ctx.matrixStack();
        VertexConsumerProvider vcp = ctx.consumers();
        if (ms == null || vcp == null) return;

        Vec3d cam = ctx.camera().getPos();
        float td = ctx.tickCounter().getTickDelta(false);
        Vec3d lp = t.getLerpedPos(td);
        long now = Util.getMeasuringTimeMs();
        int rgb = cfg.espColor.rgb();
        float h = t.getHeight(), w = Math.max(0.5f, t.getWidth());

        ms.push();
        ms.translate((float) (lp.x - cam.x), (float) (lp.y - cam.y), (float) (lp.z - cam.z));
        Matrix4f m = ms.peek().getPositionMatrix();
        VertexConsumer vc = vcp.getBuffer(WorldFx.flat());

        if (cfg.espStyle == 0) {
            float r = w * 0.5f + 0.32f;
            double phase = (now % 1700L) / 1700.0;
            for (int k = 7; k >= 0; k--) {
                double p = phase - k * 0.022;
                float y = (float) (0.5 + 0.5 * Math.sin(p * Math.PI * 2)) * (h - 0.1f);
                float a = 1f - k / 8f;
                float th = k == 0 ? 0.11f : 0.07f;
                WorldFx.band(vc, m, r, y, th, WorldFx.argb(k == 0 ? Gfx.mix(rgb, 0xFFFFFF, 0.35f) : rgb, a * (k == 0 ? 0.95f : 0.55f)), 40);
            }
        } else {
            double pulse = 0.5 + 0.5 * Math.sin(now / 380.0);
            float r = (float) (w * 0.5f + 0.42f + 0.10f * pulse);
            WorldFx.flatRing(vc, m, 0f, r - 0.10f, 0.04f, WorldFx.argb(rgb, 0.04f), WorldFx.argb(rgb, 0.22f), 44);
            WorldFx.flatRing(vc, m, r - 0.10f, r, 0.045f, WorldFx.argb(rgb, 0.90f), WorldFx.argb(rgb, 0.90f), 44);
        }
        ms.pop();

        if (vcp instanceof VertexConsumerProvider.Immediate imm) imm.draw(WorldFx.flat());
    }
}
