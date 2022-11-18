package xyz.flirora.caxton.font;

import java.util.IdentityHashMap;
import java.util.Map;

public class LayoutCache {
    private final Map<ConfiguredCaxtonFont, Map<ShapedString, ShapingResult>> shapingCache = new IdentityHashMap<>();


    public Map<ConfiguredCaxtonFont, Map<ShapedString, ShapingResult>> getShapingCache() {
        return shapingCache;
    }

    public void clear() {
        shapingCache.clear();
    }
}
