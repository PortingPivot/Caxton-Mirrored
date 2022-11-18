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

import java.util.function.Function;

@Environment(EnvType.CLIENT)
public class CaxtonTextHandler {
    private final LayoutCache cache = new LayoutCache();
    private final Function<Identifier, FontStorage> fontStorageAccessor;
    private final TextHandler vanillaHandler;

    public CaxtonTextHandler(Function<Identifier, FontStorage> fontStorageAccessor, TextHandler vanillaHandler) {
        this.fontStorageAccessor = fontStorageAccessor;
        this.vanillaHandler = vanillaHandler;
        ((TextHandlerExt) this.vanillaHandler).setCaxtonTextHandler(this);
    }

    public LayoutCache getCache() {
        return cache;
    }

    public float getWidth(int codePoint, Style style) {
        return ((TextHandlerAccessor) vanillaHandler).getWidthRetriever().getWidth(codePoint, style);
    }

    public float getWidth(@Nullable String text) {
        if (text == null) return 0.0f;

        CaxtonText runGroups = CaxtonText.fromFormatted(text, fontStorageAccessor, Style.EMPTY, false, Language.getInstance().isRightToLeft(), cache);
        return getWidth(runGroups);
    }

    public float getWidth(StringVisitable text) {
        CaxtonText runGroups = CaxtonText.fromFormatted(text, fontStorageAccessor, Style.EMPTY, false, Language.getInstance().isRightToLeft(), cache);
        return getWidth(runGroups);
    }

    public float getWidth(OrderedText text) {
        CaxtonText runGroups = CaxtonText.from(text, fontStorageAccessor, false, Language.getInstance().isRightToLeft(), cache);
        return getWidth(runGroups);
    }

    private float getWidth(CaxtonText text) {
        float total = 0;
        for (RunGroup runGroup : text.runGroups()) {
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
                    cumulWidth.add(getWidth(codePoint, style));
                    return true;
                });

                total += cumulWidth.floatValue();
            }
        } else {
            float scale = runGroup.getFont().getScale();
            ShapingResult[] shapingResults = runGroup.getShapingResults();

            for (ShapingResult shapingResult : shapingResults) {
                total += shapingResult.totalWidth() * scale;
            }
        }
        return total;
    }

    public int getCharIndexAtX(String text, int maxWidth, Style style) {
        CaxtonText runGroups = CaxtonText.fromForwards(text, fontStorageAccessor, style, false, Language.getInstance().isRightToLeft(), cache);
        return getCharIndexAtX(runGroups, maxWidth);
    }

    public int getCharIndexAtXFormatted(String text, int maxWidth, Style style) {
        CaxtonText runGroups = CaxtonText.fromFormatted(text, fontStorageAccessor, style, false, Language.getInstance().isRightToLeft(), cache);
        return getCharIndexAtX(runGroups, maxWidth);
    }

    // Gets the index of the last character that fits in a width of x
    private int getCharIndexAtX(CaxtonText text, float x) {
        int maxEnd = 0;
        for (RunGroup runGroup : text.runGroups()) {
            if (runGroup.getFont() == null) {
                int runIndex = 0;
                for (Run run : runGroup.getVisualText()) {
                    MutableFloat cumulWidth = new MutableFloat(x);
                    MutableInt theIndex = new MutableInt();
                    boolean completed = TextVisitFactory.visitForwards(run.text(), run.style(), (index, style, codePoint) -> {
                        float width = getWidth(codePoint, style);
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
                ShapingResult[] shapingResults = runGroup.getShapingResults();

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
        cache.clear();
    }
}
