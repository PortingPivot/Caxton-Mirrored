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

@Environment(EnvType.CLIENT)
public class CaxtonTypeface implements Font {
    private final ConfiguredCaxtonFont regular;
    @Nullable
    private final ConfiguredCaxtonFont bold, italic, boldItalic;

    public CaxtonTypeface(ConfiguredCaxtonFont regular, @Nullable ConfiguredCaxtonFont bold, @Nullable ConfiguredCaxtonFont italic, @Nullable ConfiguredCaxtonFont boldItalic) {
        this.regular = regular;
        this.bold = bold;
        this.italic = italic;
        this.boldItalic = boldItalic;
    }

    @Override
    public void close() {
        regular.close();
        if (bold != null) bold.close();
        if (italic != null) italic.close();
        if (boldItalic != null) boldItalic.close();
        Font.super.close();
    }

    @Nullable
    @Override
    public Glyph getGlyph(int codePoint) {
        return null;
    }

    @Override
    public IntSet getProvidedGlyphs() {
        return IntSets.emptySet();
    }

    public boolean supportsCodePoint(int codePoint, Style style) {
        return getFontByStyle(style).font().supportsCodePoint(codePoint);
    }

    public ConfiguredCaxtonFont getFontByStyle(Style style) {
        ConfiguredCaxtonFont font;
        if (style.isBold()) {
            font = style.isItalic() ? boldItalic : bold;
        } else {
            font = style.isItalic() ? italic : regular;
        }
        return font == null ? regular : font;
    }

    public Stream<ConfiguredCaxtonFont> fonts() {
        return Stream.of(regular, bold, italic, boldItalic).filter(Objects::nonNull);
    }

    public void registerFonts(TextureManager textureManager) {
        this.fonts().forEach(f -> f.font().registerTextures(textureManager));
    }
}
