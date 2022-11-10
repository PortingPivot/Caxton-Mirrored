package xyz.flirora.caxton.font;

import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.Font;
import net.minecraft.client.font.Glyph;
import net.minecraft.text.Style;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class CaxtonTypeface implements Font {
    private final CaxtonFont regular;
    @Nullable
    private final CaxtonFont bold, italic, boldItalic;

    public CaxtonTypeface(CaxtonFont regular, @Nullable CaxtonFont bold, @Nullable CaxtonFont italic, @Nullable CaxtonFont boldItalic) {
        this.regular = regular;
        this.bold = bold;
        this.italic = italic;
        this.boldItalic = boldItalic;
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
        return IntSets.emptySet();
    }

    public CaxtonFont getFontByStyle(Style style) {
        CaxtonFont font;
        if (style.isBold()) {
            font = style.isItalic() ? boldItalic : bold;
        } else {
            font = style.isItalic() ? italic : regular;
        }
        return font == null ? regular : font;
    }
}
