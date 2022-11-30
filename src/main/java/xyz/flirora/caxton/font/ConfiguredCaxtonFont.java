package xyz.flirora.caxton.font;

import com.google.gson.JsonObject;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import org.jetbrains.annotations.Nullable;
import xyz.flirora.caxton.dll.CaxtonInternal;
import xyz.flirora.caxton.layout.ShapingResult;

/**
 * A font with additional settings.
 * <p>
 * Whether an option is held by {@link CaxtonFont} or by {@link ConfiguredCaxtonFont} is determined by whether it affects MSDF generation. Options that affect MSDF generation, such as shrinkage, and those that affect atlas building, such as atlas page size, belong in {@link CaxtonFont}. Other options, such as the shadow offset or the use of OpenType features, are passed to {@link ConfiguredCaxtonFont}. To put it another way, options that are set in {@code .ttf.json} or {@code .otf.json} files are the ones passed to {@link CaxtonFont}, while those set as part of a font provider itself are the domain of {@link ConfiguredCaxtonFont}.
 *
 * @param font         the underlying {@link CaxtonFont}
 * @param ptr          the address of the pointer to the configured font in native memory
 * @param shadowOffset the shadow offset of the font
 * @see CaxtonFont
 * @see ConfiguredCaxtonFont
 */
@Environment(EnvType.CLIENT)
public record ConfiguredCaxtonFont(CaxtonFont font, long ptr, float shadowOffset) implements AutoCloseable {
    /**
     * Loads a {@link ConfiguredCaxtonFont} from its settings.
     *
     * @param manager  the {@link ResourceManager} holding the resources
     * @param settings the {@link ConfiguredCaxtonFont.Loader} that holds the settings for the font
     * @return a {@link ConfiguredCaxtonFont}
     */

    public static @Nullable ConfiguredCaxtonFont load(ResourceManager manager, @Nullable ConfiguredCaxtonFont.Loader settings) {
        if (settings == null) return null;
        String config = settings.settings == null ? null : settings.settings.toString();
        CaxtonFont font = CaxtonFontLoader.loadFontByIdentifier(manager, settings.id);
        long ptr = CaxtonInternal.configureFont(font.getFontPtr(), config);
        float shadowOffset = settings.settings == null ? 1.0f :
                JsonHelper.getFloat(settings.settings, "shadow_offset", 1.0f);
        return new ConfiguredCaxtonFont(font, ptr, shadowOffset);
    }

    /**
     * Closes this object.
     * <p>
     * This frees the native memory allocated for this object as well as freeing a reference to the underlying {@link CaxtonFont}.
     */
    @Override
    public void close() {
        CaxtonInternal.destroyConfiguredFont(ptr);
        font.close();
    }

    /**
     * Gets the scale used to convert from font units to GUI units.
     *
     * @return the factor to multiply font units by to get GUI units
     */
    public float getScale() {
        return 7.0f / font.getMetrics(CaxtonFont.Metrics.ASCENDER);
    }

    /**
     * Shapes a number of runs of text.
     *
     * @param s        the array of UTF-16 code units for the entire text
     * @param bidiRuns an array of integers consisting of interleaved {@code [start, end, level]} triples
     * @return an array of {@link ShapingResult}s whose length is {@code bidiRuns.length / 3}, such that {@code result[i]} corresponds to {@code new String(s, bidiRuns[3 * i], bidiRuns[3 * i + 1] - bidiRuns[3 * i]}
     */
    public ShapingResult[] shape(char[] s, int[] bidiRuns) {
        return CaxtonInternal.shape(ptr, s, bidiRuns);
    }

    /**
     * Settings for loading a {@link ConfiguredCaxtonFont}.
     *
     * @param id       the identifier of the {@link CaxtonFont} to use
     * @param settings the original JSON object specifying the {@link ConfiguredCaxtonFont}, or {@code null} if it was passed as an identifier alone
     */
    public record Loader(Identifier id, @Nullable JsonObject settings) {
    }
}
