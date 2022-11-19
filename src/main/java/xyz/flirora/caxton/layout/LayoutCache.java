package xyz.flirora.caxton.layout;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.flirora.caxton.font.ConfiguredCaxtonFont;

import java.util.IdentityHashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class LayoutCache {
    private final Map<ConfiguredCaxtonFont, Map<ShapedString, ShapingResult>> shapingCache = new IdentityHashMap<>();


    public Map<ConfiguredCaxtonFont, Map<ShapedString, ShapingResult>> getShapingCache() {
        return shapingCache;
    }

    public void clear() {
        shapingCache.clear();
    }
}
