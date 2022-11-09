package xyz.flirora.caxton.font;

import it.unimi.dsi.fastutil.ints.IntSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.Font;
import net.minecraft.client.font.Glyph;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class CaxtonTypeface implements Font {
    public CaxtonTypeface() {
        //
    }

    @Override
    public void close() {
        Font.super.close();
    }

    @Nullable
    @Override
    public Glyph getGlyph(int codePoint) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IntSet getProvidedGlyphs() {
        throw new UnsupportedOperationException();
    }
}
