package xyz.flirora.caxton.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.TextHandler;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;
import xyz.flirora.caxton.font.CaxtonText;
import xyz.flirora.caxton.font.LayoutCache;
import xyz.flirora.caxton.font.RunGroup;
import xyz.flirora.caxton.font.ShapingResult;
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
            MutableFloat cumulWidth = new MutableFloat();
            runGroup.accept((index, style, codePoint) -> {
                cumulWidth.add(getWidth(codePoint, style));
                return true;
            });
            total += cumulWidth.floatValue();
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
        return getCharIndexAtX(runGroups, maxWidth, 0);
    }

    public int getCharIndexAtXFormatted(String text, int maxWidth, Style style) {
        CaxtonText runGroups = CaxtonText.fromFormatted(text, fontStorageAccessor, style, false, Language.getInstance().isRightToLeft(), cache);
        return getCharIndexAtX(runGroups, maxWidth, 0);
    }

    // Gets the index of the last char that fits in a width of x, starting
    // from the char at index `from`.
    private int getCharIndexAtX(CaxtonText text, float x, int from) {
        MutableInt cpiBox = new MutableInt(from);
        for (RunGroup runGroup : text.runGroups()) {
            if (cpiBox.intValue() >= 0 && cpiBox.intValue() < runGroup.getCharOffset() && cpiBox.intValue() >= runGroup.getCharOffset() + runGroup.getTotalLength()) {
                continue;
            }
            if (runGroup.getFont() == null) {
                MutableFloat cumulWidth = new MutableFloat(x);
                MutableInt theIndex = new MutableInt();
                boolean completed = runGroup.accept((index, style, codePoint) -> {
                    if (cpiBox.intValue() >= 0 && cpiBox.intValue() != index + runGroup.getCharOffset()) {
                        return true;
                    }
                    cpiBox.setValue(-1);
                    float width = getWidth(codePoint, style);
                    if (cumulWidth.floatValue() < width) {
                        theIndex.setValue(index);
                        return false;
                    }
                    cumulWidth.subtract(width);
                    return true;
                });
                if (!completed) {
                    return theIndex.intValue();
                }
                x = cumulWidth.floatValue();
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
                    ++runIndex;
                }
            }
        }
        return text.totalLength();
    }


    public void clearCaches() {
        cache.clear();
    }
}
