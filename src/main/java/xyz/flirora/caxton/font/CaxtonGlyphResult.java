package xyz.flirora.caxton.font;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.Glyph;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public sealed interface CaxtonGlyphResult {
    @Nullable
    ConfiguredCaxtonFont getCaxtonFont();

    public static record Caxton(ConfiguredCaxtonFont font) implements CaxtonGlyphResult {
        @Override
        public @Nullable ConfiguredCaxtonFont getCaxtonFont() {
            return font;
        }
    }

    public static record Legacy(Glyph glyph) implements CaxtonGlyphResult {
        @Override
        public @Nullable ConfiguredCaxtonFont getCaxtonFont() {
            return null;
        }
    }
}
