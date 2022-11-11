package xyz.flirora.caxton.font;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Style;

/**
 * Extension interface to @link FontStorage.
 */
@Environment(EnvType.CLIENT)
public interface CaxtonFontStorage {
    CaxtonGlyphResult getCaxtonGlyph(int codePoint, boolean validateAdvance, Style style);
}
