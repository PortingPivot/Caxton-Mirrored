package xyz.flirora.caxton.font;

import com.google.gson.JsonObject;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import org.jetbrains.annotations.Nullable;
import xyz.flirora.caxton.layout.ShapingResult;

import java.io.IOException;

@Environment(EnvType.CLIENT)
public record ConfiguredCaxtonFont(CaxtonFont font, long ptr, float shadowOffset) implements AutoCloseable {

    public static @Nullable ConfiguredCaxtonFont load(ResourceManager manager, @Nullable ConfiguredCaxtonFont.Loader settings) throws IOException {
        if (settings == null) return null;
        String config = settings.settings == null ? null : settings.settings.toString();
        CaxtonFont font = CaxtonFontLoader.loadFontByIdentifier(manager, settings.id);
        long ptr = CaxtonInternal.configureFont(font.getFontPtr(), config);
        float shadowOffset = settings.settings == null ? 1.0f :
                JsonHelper.getFloat(settings.settings, "shadow_offset", 1.0f);
        return new ConfiguredCaxtonFont(font, ptr, shadowOffset);
    }

    @Override
    public void close() {
        CaxtonInternal.destroyConfiguredFont(ptr);
        font.close();
    }

    public float getScale() {
        return 7.0f / font.getMetrics(CaxtonFont.Metrics.ASCENDER);
    }

    public ShapingResult[] shape(char[] s, int[] bidiRuns) {
        return CaxtonInternal.shape(ptr, s, bidiRuns);
    }

    public record Loader(Identifier id, @Nullable JsonObject settings) {
    }
}
