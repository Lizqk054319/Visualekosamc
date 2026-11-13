package pl.holovis.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import pl.holovis.HoloConfig;
import pl.holovis.HudRenderer;
import pl.holovis.HudRenderer.Element;

/** Edytor pozycji HUD: przeciągaj FPS, koordynaty i Spotify w dowolne miejsce. */
public class HudEditScreen extends Screen {
    private final Screen parent;
    private Element dragging;
    private int dx, dy;

    public HudEditScreen(Screen parent) {
        super(Text.literal("KosaVisuals HUD"));
        this.parent = parent;
    }

    @Override public boolean shouldPause() { return false; }

    @Override
    public void close() {
        HoloConfig.save();
        if (client != null) client.setScreen(parent);
    }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        c.fill(0, 0, width, height, 0x88000000);
        String hint = "Przeciągnij elementy myszką   |   R - reset pozycji   |   L / ESC - wróć";
        c.drawText(textRenderer, hint, (width - textRenderer.getWidth(hint)) / 2, 14, 0xFFE8EEF8, false);

        for (Element e : Element.values()) {
            int[] p = HudRenderer.pixelPos(e, width, height);
            int[] s = HudRenderer.size(e);
            HudRenderer.draw(c, e, p[0], p[1], true);
            boolean on = HudRenderer.enabled(e);
            if (!on) c.fill(p[0], p[1], p[0] + s[0], p[1] + s[1], 0xAA000000);
            boolean hov = mx >= p[0] && mx < p[0] + s[0] && my >= p[1] && my < p[1] + s[1];
            int border = dragging == e ? 0xFFFFFFFF : (hov ? 0xFFCCCCCC : 0x88FFFFFF);
            c.drawBorder(p[0] - 1, p[1] - 1, s[0] + 2, s[1] + 2, border);
            c.drawText(textRenderer, e.label + (on ? "" : " (wyłączony)"), p[0], p[1] - 10, 0xFF9AA4B8, false);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;
        Element[] all = Element.values();
        for (int i = all.length - 1; i >= 0; i--) {
            Element e = all[i];
            int[] p = HudRenderer.pixelPos(e, width, height);
            int[] s = HudRenderer.size(e);
            if (mx >= p[0] && mx < p[0] + s[0] && my >= p[1] && my < p[1] + s[1]) {
                dragging = e;
                dx = (int) mx - p[0];
                dy = (int) my - p[1];
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double ddx, double ddy) {
        if (dragging == null) return false;
        int[] s = HudRenderer.size(dragging);
        int x = MathHelper.clamp((int) mx - dx, 0, Math.max(0, width - s[0]));
        int y = MathHelper.clamp((int) my - dy, 0, Math.max(0, height - s[1]));
        HudRenderer.setPos(dragging, x / (float) width, y / (float) height);
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging != null) HoloConfig.save();
        dragging = null;
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_L || key == GLFW.GLFW_KEY_RIGHT_SHIFT) { close(); return true; }
        if (key == GLFW.GLFW_KEY_R) { HudRenderer.resetPositions(); HoloConfig.save(); return true; }
        return super.keyPressed(key, scan, mods);
    }
}
