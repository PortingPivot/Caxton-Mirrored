package xyz.flirora.caxton.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.TextHandler;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.TextVisitFactory;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.Nullable;
import xyz.flirora.caxton.font.CaxtonFont;
import xyz.flirora.caxton.font.Run;
import xyz.flirora.caxton.font.RunGroup;
import xyz.flirora.caxton.font.ShapingResult;
import xyz.flirora.caxton.mixin.TextHandlerAccessor;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
public class CaxtonTextHandler {
    private final Map<CaxtonFont, Map<String, ShapingResult>> shapingCache = new IdentityHashMap<>();
    private final Function<Identifier, FontStorage> fontStorageAccessor;
    private final TextHandler vanillaHandler;

    public CaxtonTextHandler(Function<Identifier, FontStorage> fontStorageAccessor, TextHandler vanillaHandler) {
        this.fontStorageAccessor = fontStorageAccessor;
        this.vanillaHandler = vanillaHandler;
        ((TextHandlerExt) this.vanillaHandler).setCaxtonTextHandler(this);
    }

    public Map<CaxtonFont, Map<String, ShapingResult>> getShapingCache() {
        return shapingCache;
    }

    public float getWidth(@Nullable String text) {
        if (text == null) return 0.0f;

        List<RunGroup> runGroups = Run.splitIntoGroups(text, fontStorageAccessor, false);
        return getWidth(runGroups);
    }

    public float getWidth(StringVisitable text) {
        List<RunGroup> runGroups = Run.splitIntoGroups(text, fontStorageAccessor, false);
        return getWidth(runGroups);
    }

    public float getWidth(OrderedText text) {
        List<RunGroup> runGroups = Run.splitIntoGroups(text, fontStorageAccessor, false);
        return getWidth(runGroups);
    }

    private float getWidth(List<RunGroup> runGroups) {
        float total = 0;
        for (RunGroup runGroup : runGroups) {
            total += getWidth(runGroup);
        }
        return total;
    }

    private float getWidth(RunGroup runGroup) {
        float total = 0;
        if (runGroup.getFont() == null) {
            for (Run run : runGroup.getStyleRuns()) {
                MutableFloat mutableFloat = new MutableFloat();
                TextVisitFactory.visitFormatted(run.text(), run.style(), (unused, style, codePoint) -> {
                    mutableFloat.add(((TextHandlerAccessor) vanillaHandler).getWidthRetriever().getWidth(codePoint, style));
                    return true;
                });

                total += mutableFloat.floatValue();
            }
        } else {
            float scale = 7.0f / runGroup.getFont().getMetrics(CaxtonFont.Metrics.ASCENDER);
            ShapingResult[] shapingResults = runGroup.shape(this.getShapingCache());

            System.out.println(Arrays.toString(shapingResults));
            for (ShapingResult shapingResult : shapingResults) {
                total += shapingResult.totalWidth() * scale;
            }
        }
        return total;
    }

    public void clearCaches() {
        shapingCache.clear();
    }
}
