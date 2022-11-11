package xyz.flirora.caxton.font;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.Glyph;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public sealed interface CaxtonGlyphResult {
    @Nullable
    CaxtonFont getCaxtonFont();

    public static record Caxton(CaxtonFont font) implements CaxtonGlyphResult {
        @Override
        public @Nullable CaxtonFont getCaxtonFont() {
            return font;
        }
    }

    public static record Legacy(Glyph glyph) implements CaxtonGlyphResult {
        @Override
        public @Nullable CaxtonFont getCaxtonFont() {
            return null;
        }
    }
}
