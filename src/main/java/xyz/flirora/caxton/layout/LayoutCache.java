package xyz.flirora.caxton.layout;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.flirora.caxton.font.ConfiguredCaxtonFont;

import java.time.Duration;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class LayoutCache {
    private static final LayoutCache INSTANCE = new LayoutCache();
    private final Map<ConfiguredCaxtonFont, Cache<ShapedString, ShapingResult>> shapingCaches = new IdentityHashMap<>();
    private final Cache<FromRunsInput, CaxtonText.Full> reorderCache;

    private LayoutCache() {
        reorderCache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterAccess(Duration.ofMinutes(1))
                .build();
    }

    public static LayoutCache getInstance() {
        return INSTANCE;
    }

    public Map<ConfiguredCaxtonFont, Cache<ShapedString, ShapingResult>> getShapingCaches() {
        return shapingCaches;
    }

    public Cache<ShapedString, ShapingResult> getShapingCacheFor(ConfiguredCaxtonFont font) {
        return shapingCaches.computeIfAbsent(
                font,
                f -> Caffeine.newBuilder()
                        .maximumSize(10_000)
                        .expireAfterAccess(Duration.ofMinutes(1))
                        .build());
    }

    public Cache<FromRunsInput, CaxtonText.Full> getReorderCache() {
        return reorderCache;
    }

    public void clear() {
        shapingCaches.clear();
        reorderCache.invalidateAll();
    }

    public record FromRunsInput(List<Run> runs, boolean rtl) {
    }
}
