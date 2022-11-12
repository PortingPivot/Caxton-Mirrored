package xyz.flirora.caxton.mixin;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.BuiltinEmptyGlyph;
import net.minecraft.client.font.Font;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.Glyph;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.flirora.caxton.font.CaxtonFontStorage;
import xyz.flirora.caxton.font.CaxtonGlyphPair;
import xyz.flirora.caxton.font.CaxtonGlyphResult;
import xyz.flirora.caxton.font.CaxtonTypeface;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Environment(EnvType.CLIENT)
@Mixin(FontStorage.class)
public abstract class FontStorageMixin implements AutoCloseable, CaxtonFontStorage {
    private Int2ObjectMap<CaxtonGlyphPair>[] caxtonGlyphCache = null;
    @Shadow
    @Final
    private List<Font> fonts;
    @Shadow
    @Final
    private TextureManager textureManager;

    @Shadow
    private static boolean isAdvanceInvalid(Glyph glyph) {
        throw new UnsupportedOperationException("this is a mixin, silly");
    }

    @Inject(at = @At(value = "TAIL"), method = "<init>(Lnet/minecraft/client/texture/TextureManager;Lnet/minecraft/util/Identifier;)V")
    @SuppressWarnings("unchecked")
    private void init(CallbackInfo ci) {
        caxtonGlyphCache = new Int2ObjectMap[4];
        for (int i = 0; i < 4; ++i) {
            caxtonGlyphCache[i] = new Int2ObjectOpenHashMap<>();
        }
    }

    @Inject(at = @At(value = "HEAD"), method = "setFonts(Ljava/util/List;)V")
    private void onSetFonts(List<Font> fonts, CallbackInfo ci) {
        for (Int2ObjectMap<CaxtonGlyphPair> cacheEntry : caxtonGlyphCache) {
            cacheEntry.clear();
        }
    }

    private Int2ObjectMap<CaxtonGlyphPair> getCacheForStyle(Style style) {
        return caxtonGlyphCache[(style.isBold() ? 2 : 0) | (style.isItalic() ? 1 : 0)];
    }

    // Always retain Caxton fonts in the set of used fonts.
    // Caxton fonts always claim to contain no glyphs, so they would otherwise
    // not end up in the FontStorage#fonts field.
    @Redirect(method = "setFonts(Ljava/util/List;)V", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;filter(Ljava/util/function/Predicate;)Ljava/util/stream/Stream;"))
    private Stream<Font> filterProxy(Stream<Font> allFonts, Predicate<Font> predicate) {
        return allFonts.filter(font -> {
            if (font instanceof CaxtonTypeface c) {
                c.registerFonts(this.textureManager);
                return true;
            }
            return predicate.test(font);
        });
    }

    // Based on the findGlyph method.
    private CaxtonGlyphPair findCaxtonGlyph(int codePoint, Style style) {
        Glyph glyph = null;
        for (Font font : this.fonts) {
            if (font instanceof CaxtonTypeface caxtonFont && caxtonFont.supportsCodePoint(codePoint, style)) {
                return new CaxtonGlyphPair.Caxton(caxtonFont.getFontByStyle(style));
            }
            Glyph glyph2 = font.getGlyph(codePoint);
            if (glyph2 == null) continue;
            if (glyph == null) {
                glyph = glyph2;
            }
            if (isAdvanceInvalid(glyph2)) continue;
            return new CaxtonGlyphPair.Legacy(glyph, glyph2);
        }
        if (glyph != null) {
            return new CaxtonGlyphPair.Legacy(glyph, BuiltinEmptyGlyph.MISSING);
        }
        return CaxtonGlyphPair.MISSING;
    }

    @Override
    public CaxtonGlyphResult getCaxtonGlyph(int codePoint, boolean validateAdvance, Style style) {
        return getCacheForStyle(style).computeIfAbsent(codePoint, c -> findCaxtonGlyph(c, style)).getGlyph(validateAdvance);
    }
}
