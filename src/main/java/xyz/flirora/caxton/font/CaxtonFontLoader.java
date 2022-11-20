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

/**
 * A {@link FontLoader} for the {@code caxton} font type.
 */
@Environment(EnvType.CLIENT)
public class CaxtonFontLoader implements FontLoader {
    public static final String FONT_PREFIX = "textures/font/";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<Identifier, CaxtonFont> CACHE = new HashMap<>();
    private static final JsonObject EMPTY = new JsonObject();
    private final ConfiguredCaxtonFont.Loader regular;
    @Nullable
    private final ConfiguredCaxtonFont.Loader bold, italic, boldItalic;

    /**
     * Constructs a new {@link CaxtonFontLoader}.
     *
     * @param regular    the {@link ConfiguredCaxtonFont.Loader} to use for the regular style
     * @param bold       the {@link ConfiguredCaxtonFont.Loader} to use for the bold style, or null to fall back to the regular style
     * @param italic     the {@link ConfiguredCaxtonFont.Loader} to use for the italic style, or null to fall back to the regular style
     * @param boldItalic the {@link ConfiguredCaxtonFont.Loader} to use for the bold italic style, or null to fall back to the regular style
     */
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

    /**
     * Constructs a {@link CaxtonFontLoader} from a JSON object.
     *
     * @param json the JSON object describing the font settings
     * @return a {@link CaxtonFontLoader}
     */
    public static FontLoader fromJson(JsonObject json) {
        return new CaxtonFontLoader(
                parseConfiguredFontLoader(json.get("regular")),
                parseConfiguredFontLoader(json.get("bold")),
                parseConfiguredFontLoader(json.get("italic")),
                parseConfiguredFontLoader(json.get("bold_italic")));
    }

    /**
     * Loads a {@link CaxtonFont} by its identifier, or gets a cached copy if it has already been loaded.
     *
     * @param manager the {@link ResourceManager} holding the resources
     * @param id      the {@link Identifier} to load the font by
     * @return a {@link CaxtonFont}, with its reference count appropriately updated
     */
    @Nullable
    public static CaxtonFont loadFontByIdentifier(ResourceManager manager, @Nullable Identifier id) {
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

    /**
     * Clears the font cache used by {@link CaxtonFontLoader#loadFontByIdentifier(ResourceManager, Identifier)}.
     */
    public static void clearFontCache() {
        CACHE.clear();
    }

    /**
     * Gets a font by its identifier.
     *
     * @param id the {@link Identifier} that the font was loaded by
     * @return the {@link CaxtonFont} associated with {@code id}, or {@code null} if none. The reference count is not incremented.
     */
    public static CaxtonFont getFontById(Identifier id) {
        return CACHE.get(id);
    }

    /**
     * Loads the typeface described by this object.
     *
     * @param manager the {@link ResourceManager} holding the resources
     * @return a {@link CaxtonTypeface} described by this object
     */
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
