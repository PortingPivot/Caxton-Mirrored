package xyz.flirora.caxton.font;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.Font;
import net.minecraft.client.font.FontLoader;
import net.minecraft.resource.Resource;
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
import java.util.Optional;

@Environment(EnvType.CLIENT)
public class CaxtonFontLoader implements FontLoader {
    public static final String FONT_PREFIX = "textures/font/";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<Identifier, CaxtonFont> CACHE = new HashMap<>();
    private static final JsonObject EMPTY = new JsonObject();
    private final ConfiguredCaxtonFont.Loader regular;
    @Nullable
    private final ConfiguredCaxtonFont.Loader bold, italic, boldItalic;

    public CaxtonFontLoader(ConfiguredCaxtonFont.Loader regular, ConfiguredCaxtonFont.@Nullable Loader bold, ConfiguredCaxtonFont.@Nullable Loader italic, ConfiguredCaxtonFont.@Nullable Loader boldItalic) {
        this.regular = regular;
        this.bold = bold;
        this.italic = italic;
        this.boldItalic = boldItalic;
    }

    private static @Nullable ConfiguredCaxtonFont.Loader parseConfiguredFontLoader(@Nullable JsonElement element) {
        if (element == null) return null;
        if (element instanceof JsonObject object) {
            return new ConfiguredCaxtonFont.Loader(
                    Identifier.tryParse(JsonHelper.getString(object, "file")),
                    object
            );
        } else if (element instanceof JsonPrimitive primitive) {
            return new ConfiguredCaxtonFont.Loader(Identifier.tryParse(primitive.getAsString()), null);
        }
        throw new JsonParseException("expected identifier or object; got something else");
    }

    private static @Nullable Identifier getOptionalIdentifier(JsonObject json, String key) {
        String id = JsonHelper.getString(json, key, null);
        return id == null ? null : new Identifier(id);
    }

    public static FontLoader fromJson(JsonObject json) {
        return new CaxtonFontLoader(
                parseConfiguredFontLoader(json.get("regular")),
                parseConfiguredFontLoader(json.get("bold")),
                parseConfiguredFontLoader(json.get("italic")),
                parseConfiguredFontLoader(json.get("bold_italic")));
    }

    @Nullable
    public static CaxtonFont loadFontByIdentifier(ResourceManager manager, @Nullable Identifier id) throws IOException {
        if (id == null) return null;
        return CACHE.computeIfAbsent(id, id1 -> {
            try {
                Identifier fontId = id1.withPrefixedPath(FONT_PREFIX);
                Identifier metaId = fontId.withPath(path -> path + ".json");
                JsonObject optionsJson = EMPTY;
                Optional<Resource> metaResource = manager.getResource(metaId);
                if (metaResource.isPresent()) {
                    try (BufferedReader metaInput = metaResource.get().getReader()) {
                        optionsJson = JsonHelper.deserialize(metaInput);
                        String path = JsonHelper.getString(optionsJson, "path", null);
                        if (path != null) {
                            fontId = new Identifier(path).withPrefixedPath(FONT_PREFIX);
                        }
                    }
                }
                try (InputStream input = manager.open(fontId)) {
                    return new CaxtonFont(input, id1, optionsJson);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).cloneReference();
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
            ConfiguredCaxtonFont regular = ConfiguredCaxtonFont.load(manager, this.regular);
            ConfiguredCaxtonFont bold = ConfiguredCaxtonFont.load(manager, this.bold);
            ConfiguredCaxtonFont italic = ConfiguredCaxtonFont.load(manager, this.italic);
            ConfiguredCaxtonFont boldItalic = ConfiguredCaxtonFont.load(manager, this.boldItalic);
            return new CaxtonTypeface(regular, bold, italic, boldItalic);
        } catch (Exception exception) {
            LOGGER.error("Couldn't load truetype font", exception);
        }
        return null;
    }
}
