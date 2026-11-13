package pl.holovis;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class HoloConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("holovis.json");
    private static HoloConfig instance = new HoloConfig();

    public enum HitboxStyle {
        OUTLINE("Kontur"), HOLOGRAM("Hologram"), FILLED("Wypełnienie");
        public final String label;
        HitboxStyle(String l) { label = l; }
        public HitboxStyle next() { return values()[(ordinal() + 1) % values().length]; }
    }

    public enum Target {
        ALL("Wszystkie"), PLAYERS("Gracze"), MOBS("Moby");
        public final String label;
        Target(String l) { label = l; }
        public Target next() { return values()[(ordinal() + 1) % values().length]; }
    }

    public static class ColorSetting {
        public float h, s = 1f, b = 1f;
        public boolean rainbow = false;

        public ColorSetting() {}
        public ColorSetting(int rgb) { set(rgb); }

        public void set(int rgb) {
            float[] hsv = toHsv(rgb);
            h = hsv[0]; s = hsv[1]; b = hsv[2];
        }

        /** Aktualny kolor RGB (0xRRGGBB), z uwzględnieniem trybu rainbow. */
        public int rgb() {
            if (!rainbow) return hsv(h, s, b);
            // tęcza: przy bladych kolorach (np. biały napis) wymuszamy nasycenie, żeby było ją widać
            float hue = (Util.getMeasuringTimeMs() % 6000L) / 6000f;
            return hsv(hue, Math.max(s, 0.65f), Math.max(b, 0.8f));
        }
    }

    public static class SoundSetting {
        public boolean enabled = false;
        public String sound = "mc:entity.experience_orb.pickup";
        public float volume = 0.8f;
        public float pitch = 1f;

        public SoundSetting() {}
        public SoundSetting(boolean enabled, String sound) { this.enabled = enabled; this.sound = sound; }
    }

    public boolean hitboxEnabled = true;
    public HitboxStyle hitboxStyle = HitboxStyle.HOLOGRAM;
    public Target target = Target.ALL;
    public float fillAlpha = 0.25f;
    public ColorSetting hitboxColor = new ColorSetting(0x00F0FF);

    public ColorSetting guiColor = new ColorSetting(0x00F0FF);
    public boolean hologramFx = true;
    public ColorSetting guiColor2 = new ColorSetting(0xB04CFF);
    public boolean guiGradient = false;
    public boolean guiGradientText = false;
    public ColorSetting guiBg = new ColorSetting(0x0A1020);
    public float guiBgAlpha = 0.88f;
    public ColorSetting textColor = new ColorSetting(0xE8EEF8);

    public boolean skyEnabled = false;
    public int skyPreset = 0;   // 0 = własny, reszta: SkyHelper.PRESETS
    public ColorSetting skyColor = new ColorSetting(0x8A2BE2);
    public ColorSetting skyColor2 = new ColorSetting(0xC890FF);   // horyzont
    public boolean skyDaylight = true;
    public boolean skyClouds = true;

    public boolean watermark = true;
    public boolean showFps = true;
    public boolean coords = true;
    public ColorSetting hudColor = new ColorSetting(0x00F0FF);
    public ColorSetting hudColor2 = new ColorSetting(0xB04CFF);
    public boolean hudGradient = false;
    public boolean spotify = false;
    public ColorSetting spColor = new ColorSetting(0x1DB954);
    public ColorSetting spColor2 = new ColorSetting(0x7CFFB0);
    public boolean spGradient = false;

    public boolean cooldowns = true;
    public ColorSetting cdColor = new ColorSetting(0xA45CFF);
    public float cdX = 0.01f, cdY = 0.30f;

    public boolean targetEsp = false;
    public int espStyle = 0;   // 0 pierścień, 1 okrąg pod nogami
    public ColorSetting espColor = new ColorSetting(0xB04CFF);

    public boolean dmgNumbers = false;
    public float dmgSize = 1.0f;
    public ColorSetting dmgColor = new ColorSetting(0xFF4D5E);

    /** Wersja wyglądu menu: starsze configi dostają nowy, ciemny styl "glass". */
    public int uiVer = 0;

    public boolean targetHud = true, targetHearts = true, targetArmor = true;
    public ColorSetting tgColor = new ColorSetting(0x00F0FF);
    public ColorSetting tgColor2 = new ColorSetting(0xB04CFF);
    public boolean tgGradient = true;
    public float tgX = 0.55f, tgY = 0.55f;

    public boolean killFx = false;
    public int killMode = 0; // 0 duch, 1 piorun, 2 oba
    public float killGhostSecs = 3f;
    public ColorSetting killGhostColor = new ColorSetting(0x9AD8FF);
    public float killBoltHeight = 12f;
    public ColorSetting killBoltColor = new ColorSetting(0xB9A3FF);
    public boolean killMobs = false;

    public float wmX = 0.01f, wmY = 0.02f, spX = 0.01f, spY = 0.09f;

    public SoundSetting hitSound = new SoundSetting(true, "mc:entity.experience_orb.pickup");
    public SoundSetting killSound = new SoundSetting(false, "mc:entity.player.levelup");
    public SoundSetting clickSound = new SoundSetting(false, "mc:block.note_block.pling");

    public static HoloConfig get() { return instance; }

    /** Kod do udostępniania ustawień. Format: HV1:base64(gzip(json)). */
    public static String exportCode() {
        try {
            JsonObject o = GSON.toJsonTree(instance).getAsJsonObject();
            byte[] raw = new Gson().toJson(o).getBytes(StandardCharsets.UTF_8);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (GZIPOutputStream gz = new GZIPOutputStream(bos)) { gz.write(raw); }
            return "HV1:" + Base64.getUrlEncoder().withoutPadding().encodeToString(bos.toByteArray());
        } catch (Exception e) {
            return "";
        }
    }

    /** @return null gdy się udało, w przeciwnym razie komunikat błędu */
    public static String importCode(String code) {
        try {
            code = code == null ? "" : code.trim();
            if (!code.startsWith("HV1:")) return "To nie jest kod KosaVisuals";
            byte[] gz = Base64.getUrlDecoder().decode(code.substring(4).trim());
            String json = new String(new GZIPInputStream(new ByteArrayInputStream(gz)).readAllBytes(), StandardCharsets.UTF_8);
            HoloConfig loaded = GSON.fromJson(json, HoloConfig.class);
            if (loaded == null) return "Pusty kod";
            instance = loaded;
            save();
            return null;
        } catch (Exception e) {
            return "Niepoprawny kod";
        }
    }

    public static void load() {
        try {
            if (Files.exists(PATH)) {
                HoloConfig loaded = GSON.fromJson(Files.readString(PATH), HoloConfig.class);
                if (loaded != null) instance = loaded;
            }
            if (instance.uiVer < 2) {
                instance.uiVer = 2;
                instance.guiColor = new ColorSetting(0x4C8DFF);
                instance.guiColor2 = new ColorSetting(0xA05CFF);
                instance.guiGradient = false;
                instance.guiGradientText = false;
                instance.guiBg = new ColorSetting(0x1A2230);
                instance.guiBgAlpha = 0.80f;
                instance.textColor = new ColorSetting(0xE8EEF8);
                instance.hologramFx = false;
            }
        } catch (Exception e) {
            System.err.println("[KosaVisuals] Nie udało się wczytać configu: " + e);
        }
        save();
    }

    public static void save() {
        try {
            Files.writeString(PATH, GSON.toJson(instance));
        } catch (Exception e) {
            System.err.println("[KosaVisuals] Nie udało się zapisać configu: " + e);
        }
    }

    // ---- HSV helpers ----
    public static int hsv(float h, float s, float v) {
        h = h - (float) Math.floor(h);
        int i = (int) (h * 6);
        float f = h * 6 - i;
        float p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s);
        float r, g, b;
        switch (i % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }

    public static float[] toHsv(int rgb) {
        float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        float d = max - min, h;
        if (d == 0) h = 0;
        else if (max == r) h = ((g - b) / d) / 6f;
        else if (max == g) h = (2 + (b - r) / d) / 6f;
        else h = (4 + (r - g) / d) / 6f;
        if (h < 0) h += 1;
        return new float[]{h, max == 0 ? 0 : d / max, max};
    }
}
