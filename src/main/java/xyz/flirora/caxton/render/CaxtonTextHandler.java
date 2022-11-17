package xyz.flirora.caxton.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.TextHandler;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.TextVisitFactory;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;
import xyz.flirora.caxton.font.*;
import xyz.flirora.caxton.mixin.TextHandlerAccessor;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
public class CaxtonTextHandler {
    private final Map<ConfiguredCaxtonFont, Map<ShapedString, ShapingResult>> shapingCache = new IdentityHashMap<>();
    private final Function<Identifier, FontStorage> fontStorageAccessor;
    private final TextHandler vanillaHandler;

    public CaxtonTextHandler(Function<Identifier, FontStorage> fontStorageAccessor, TextHandler vanillaHandler) {
        this.fontStorageAccessor = fontStorageAccessor;
        this.vanillaHandler = vanillaHandler;
        ((TextHandlerExt) this.vanillaHandler).setCaxtonTextHandler(this);
    }

    public Map<ConfiguredCaxtonFont, Map<ShapedString, ShapingResult>> getShapingCache() {
        return shapingCache;
    }

    public float getWidth(@Nullable String text) {
        if (text == null) return 0.0f;

        List<RunGroup> runGroups = Run.splitIntoGroupsFormatted(text, fontStorageAccessor, Style.EMPTY, false, Language.getInstance().isRightToLeft());
        return getWidth(runGroups);
    }

    public float getWidth(StringVisitable text) {
        List<RunGroup> runGroups = Run.splitIntoGroupsFormatted(text, fontStorageAccessor, Style.EMPTY, false, Language.getInstance().isRightToLeft());
        return getWidth(runGroups);
    }

    public float getWidth(OrderedText text) {
        List<RunGroup> runGroups = Run.splitIntoGroups(text, fontStorageAccessor, false, Language.getInstance().isRightToLeft());
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
            for (Run run : runGroup.getVisualText()) {
                MutableFloat cumulWidth = new MutableFloat();
                TextVisitFactory.visitFormatted(run.text(), run.style(), (unused, style, codePoint) -> {
                    cumulWidth.add(((TextHandlerAccessor) vanillaHandler).getWidthRetriever().getWidth(codePoint, style));
                    return true;
                });

                total += cumulWidth.floatValue();
            }
        } else {
            float scale = runGroup.getFont().getScale();
            ShapingResult[] shapingResults = runGroup.shape(this.getShapingCache());

            for (ShapingResult shapingResult : shapingResults) {
                total += shapingResult.totalWidth() * scale;
            }
        }
        return total;
    }

    public int getCharIndexAtX(String text, int maxWidth, Style style) {
        List<RunGroup> runGroups = Run.splitIntoGroupsForwards(text, fontStorageAccessor, style, false, Language.getInstance().isRightToLeft());
        return getCharIndexAtX(runGroups, maxWidth);
    }

    public int getCharIndexAtXFormatted(String text, int maxWidth, Style style) {
        List<RunGroup> runGroups = Run.splitIntoGroupsFormatted(text, fontStorageAccessor, style, false, Language.getInstance().isRightToLeft());
        return getCharIndexAtX(runGroups, maxWidth);
    }

    // Gets the index of the last character that fits in a width of x
    private int getCharIndexAtX(List<RunGroup> runGroups, float x) {
        int maxEnd = 0;
        for (RunGroup runGroup : runGroups) {
            if (runGroup.getFont() == null) {
                int runIndex = 0;
                for (Run run : runGroup.getVisualText()) {
                    MutableFloat cumulWidth = new MutableFloat(x);
                    MutableInt theIndex = new MutableInt();
                    boolean completed = TextVisitFactory.visitForwards(run.text(), run.style(), (index, style, codePoint) -> {
                        float width = ((TextHandlerAccessor) vanillaHandler).getWidthRetriever().getWidth(codePoint, style);
                        if (cumulWidth.floatValue() < width) {
                            theIndex.setValue(index);
                            return false;
                        }
                        cumulWidth.subtract(width);
                        return true;
                    });
                    if (!completed) {
                        // In the RTL case, this is a best-faith approximation
                        // assuming that legacy layout has not changed the
                        // number of characters in the run.
                        int[] bidiRuns = runGroup.getBidiRuns();
                        int start = bidiRuns[3 * runIndex];
                        int end = bidiRuns[3 * runIndex + 1];
                        int level = bidiRuns[3 * runIndex + 2];
                        return level % 2 != 0 ?
                                runGroup.getCharOffset() + Math.max(start, end - theIndex.intValue()) :
                                runGroup.getCharOffset() + Math.min(end, start + theIndex.intValue());
                    }
                    x = cumulWidth.floatValue();
                    maxEnd = Math.max(maxEnd, runGroup.getCharOffset() + runGroup.getBidiRuns()[3 * runIndex + 1]);
                    ++runIndex;
                }
            } else {
                float scale = runGroup.getFont().getScale();
                ShapingResult[] shapingResults = runGroup.shape(this.getShapingCache());

                int runIndex = 0;
                for (ShapingResult shapingResult : shapingResults) {
                    for (int i = 0; i < shapingResult.numGlyphs(); ++i) {
                        float width = scale * shapingResult.advanceX(i);
                        if (x < width) {
                            int[] bidiRuns = runGroup.getBidiRuns();
                            int start = bidiRuns[3 * runIndex];
                            return runGroup.getCharOffset() + start + shapingResult.clusterIndex(i);
                        }
                        x -= width;
                    }
                    maxEnd = Math.max(maxEnd, runGroup.getCharOffset() + runGroup.getBidiRuns()[3 * runIndex + 1]);
                    ++runIndex;
                }
            }
        }
        return maxEnd;
    }


    public void clearCaches() {
        shapingCache.clear();
    }
}
