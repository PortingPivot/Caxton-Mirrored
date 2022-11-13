package xyz.flirora.caxton.font;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.Font;
import net.minecraft.client.font.FontLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class CaxtonFontLoader implements FontLoader {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<Identifier, CaxtonFont> CACHE = new HashMap<>();

    private final Identifier regular;
    @Nullable
    private final Identifier bold, italic, boldItalic;
    private final double size;

    public CaxtonFontLoader(Identifier regular, @Nullable Identifier bold, @Nullable Identifier italic, @Nullable Identifier boldItalic, double size) {
        this.regular = regular;
        this.bold = bold;
        this.italic = italic;
        this.boldItalic = boldItalic;
        this.size = size;
    }

    private static @Nullable Identifier getOptionalIdentifier(JsonObject json, String key) {
        String id = JsonHelper.getString(json, key, null);
        return id == null ? null : new Identifier(id);
    }

    public static FontLoader fromJson(JsonObject json) {
        return new CaxtonFontLoader(
                new Identifier(JsonHelper.getString(json, "regular")),
                getOptionalIdentifier(json, "bold"),
                getOptionalIdentifier(json, "italic"),
                getOptionalIdentifier(json, "bold_italic"),
                JsonHelper.getDouble(json, "size", 11.0));
    }

    @Nullable
    private static CaxtonFont loadFontByIdentifier(ResourceManager manager, @Nullable Identifier id) throws IOException {
        if (id == null) return null;
        return CACHE.computeIfAbsent(id, id1 -> {
            Identifier fontId = id1.withPrefixedPath("textures/font/");
            Identifier metaId = fontId.withPath(path -> path + ".json");
            try (InputStream input = manager.open(fontId);
                 BufferedReader metaInput = manager.getResourceOrThrow(metaId).getReader()) {
                JsonObject optionsJson = JsonHelper.deserialize(metaInput);
                return new CaxtonFont(input, id1, optionsJson);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void clearFontCache() {
        CACHE.clear();
    }

    public static CaxtonFont getFontById(Identifier id) {
        return CACHE.get(id);
    }

    @Nullable
    @Override
    public Font load(ResourceManager manager) {
        try {
            CaxtonFont regular = loadFontByIdentifier(manager, this.regular);
            CaxtonFont bold = loadFontByIdentifier(manager, this.bold);
            CaxtonFont italic = loadFontByIdentifier(manager, this.italic);
            CaxtonFont boldItalic = loadFontByIdentifier(manager, this.boldItalic);
            return new CaxtonTypeface(regular, bold, italic, boldItalic);
        } catch (Exception exception) {
            LOGGER.error("Couldn't load truetype font", exception);
        }
        return null;
    }
}
