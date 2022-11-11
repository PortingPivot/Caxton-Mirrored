package xyz.flirora.caxton.font;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.BuiltinEmptyGlyph;
import net.minecraft.client.font.Glyph;

@Environment(EnvType.CLIENT)
public interface CaxtonGlyphPair {
    static final CaxtonGlyphPair MISSING = new Legacy(BuiltinEmptyGlyph.MISSING, BuiltinEmptyGlyph.MISSING);

    CaxtonGlyphResult getGlyph(boolean advanceValidating);

    public static record Caxton(CaxtonFont font) implements CaxtonGlyphPair {
        @Override
        public CaxtonGlyphResult getGlyph(boolean advanceValidating) {
            return new CaxtonGlyphResult.Caxton(font);
        }
    }

    public static record Legacy(Glyph glyph, Glyph advanceValidatingGlyph) implements CaxtonGlyphPair {
        @Override
        public CaxtonGlyphResult getGlyph(boolean advanceValidating) {
            return new CaxtonGlyphResult.Legacy(advanceValidating ? advanceValidatingGlyph : glyph);
        }
    }
}
