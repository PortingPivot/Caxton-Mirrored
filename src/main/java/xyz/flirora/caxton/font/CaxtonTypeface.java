package xyz.flirora.caxton.font;

import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.Font;
import net.minecraft.client.font.Glyph;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.text.Style;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * Contains {@link ConfiguredCaxtonFont}s for different styles.
 * <p>
 * Caxton does not simulate bold and italic styles in the way that vanilla Minecraft does; instead, it supports loading the font files for these styles.
 *
 * @see CaxtonFont
 * @see ConfiguredCaxtonFont
 */
@Environment(EnvType.CLIENT)
public class CaxtonTypeface implements Font {
    private final ConfiguredCaxtonFont regular;
    @Nullable
    private final ConfiguredCaxtonFont bold, italic, boldItalic;

    /**
     * Creates a new {@link CaxtonTypeface}.
     *
     * @param regular    the {@link ConfiguredCaxtonFont} to use for the regular style
     * @param bold       the {@link ConfiguredCaxtonFont} to use for the bold style. This will fall back to {@code regular} if null.
     * @param italic     the {@link ConfiguredCaxtonFont} to use for the italic style. This will fall back to {@code regular} if null.
     * @param boldItalic the {@link ConfiguredCaxtonFont} to use for the bold italic style. This will fall back to {@code regular} if null.
     */
    public CaxtonTypeface(ConfiguredCaxtonFont regular, @Nullable ConfiguredCaxtonFont bold, @Nullable ConfiguredCaxtonFont italic, @Nullable ConfiguredCaxtonFont boldItalic) {
        this.regular = regular;
        this.bold = bold;
        this.italic = italic;
        this.boldItalic = boldItalic;
    }

    /**
     * Closes this {@link CaxtonTypeface}.
     */
    @Override
    public void close() {
        regular.close();
        if (bold != null) bold.close();
        if (italic != null) italic.close();
        if (boldItalic != null) boldItalic.close();
        Font.super.close();
    }

    /**
     * Stub to tell {@link net.minecraft.client.font.FontStorage} that we do not provide any legacy glyphs.
     *
     * @param codePoint the code point of the {@link Glyph} to get
     * @return {@code null}
     */
    @Nullable
    @Override
    public Glyph getGlyph(int codePoint) {
        return null;
    }

    /**
     * Stub to tell {@link net.minecraft.client.font.FontStorage} that we do not provide any legacy glyphs.
     *
     * @return an empty {@link IntSet}
     */
    @Override
    public IntSet getProvidedGlyphs() {
        return IntSets.emptySet();
    }

    /**
     * Returns whether this typeface supports the given codepoint in the given style.
     *
     * @param codePoint a Unicode code point
     * @param style     the {@link Style} to query under
     * @return true if the code point and style are supported by this typeface; false otherwise
     */
    public boolean supportsCodePoint(int codePoint, Style style) {
        return getFontByStyle(style).font().supportsCodePoint(codePoint);
    }

    /**
     * Gets the font to use for the given style.
     *
     * @param style a {@link Style}
     * @return the {@link ConfiguredCaxtonFont} to use for this style
     */
    public ConfiguredCaxtonFont getFontByStyle(Style style) {
        ConfiguredCaxtonFont font;
        if (style.isBold()) {
            font = style.isItalic() ? boldItalic : bold;
        } else {
            font = style.isItalic() ? italic : regular;
        }
        return font == null ? regular : font;
    }

    /**
     * Gets the fonts used by this typeface as a stream.
     *
     * @return a {@link Stream} of the {@link ConfiguredCaxtonFont}s used
     */
    public Stream<ConfiguredCaxtonFont> fonts() {
        return Stream.of(regular, bold, italic, boldItalic).filter(Objects::nonNull);
    }

    /**
     * Registers all fonts used by this typeface.
     *
     * @param textureManager the {@link TextureManager} to register the font textures under
     */
    public void registerFonts(TextureManager textureManager) {
        this.fonts().forEach(f -> f.font().registerTextures(textureManager));
    }
}
