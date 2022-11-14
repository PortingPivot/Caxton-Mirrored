package xyz.flirora.caxton.font;

import com.google.gson.JsonObject;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

@Environment(EnvType.CLIENT)
public record ConfiguredCaxtonFont(CaxtonFont font, long ptr) implements AutoCloseable {

    public static @Nullable ConfiguredCaxtonFont load(ResourceManager manager, @Nullable ConfiguredCaxtonFont.Loader settings) throws IOException {
        if (settings == null) return null;
        String config = settings.settings == null ? null : settings.settings.toString();
        CaxtonFont font = CaxtonFontLoader.loadFontByIdentifier(manager, settings.id);
        long ptr = CaxtonInternal.configureFont(font.getFontPtr(), config);
        return new ConfiguredCaxtonFont(font, ptr);
    }

    @Override
    public void close() {
        CaxtonInternal.destroyConfiguredFont(ptr);
        font.close();
    }

    public ShapingResult[] shape(char[] s, int[] bidiRuns) {
        return CaxtonInternal.shape(ptr, s, bidiRuns);
    }

    public record Loader(Identifier id, @Nullable JsonObject settings) {
    }
}
