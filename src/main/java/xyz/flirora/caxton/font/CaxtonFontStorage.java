package xyz.flirora.caxton.font;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Style;

/**
 * Extension interface to {@link net.minecraft.client.font.FontStorage}.
 * <p>
 * This is implemented by {@link xyz.flirora.caxton.mixin.FontStorageMixin}.
 */
@Environment(EnvType.CLIENT)
public interface CaxtonFontStorage {
    /**
     * Gets a {@link CaxtonGlyphResult} for the codepoint.
     *
     * @param codePoint       the code point to get the glyph for
     * @param validateAdvance whether to validate the advance of the codepoint. Currently, this is not done for Caxton font glyphs.
     * @param style           the style to get the glyph for
     * @return a {@link CaxtonGlyphResult} describing the glyph used for the given codepoint and style
     */
    CaxtonGlyphResult getCaxtonGlyph(int codePoint, boolean validateAdvance, Style style);
}
