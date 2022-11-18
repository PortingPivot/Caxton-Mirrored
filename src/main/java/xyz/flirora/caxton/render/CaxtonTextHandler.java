package xyz.flirora.caxton.render;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
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

    public float getWidth(CaxtonText text) {
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
        return getCharIndexAtX(runGroups, maxWidth, -1);
    }

    public int getCharIndexAtXFormatted(String text, int maxWidth, Style style) {
        CaxtonText runGroups = CaxtonText.fromFormatted(text, fontStorageAccessor, style, false, Language.getInstance().isRightToLeft(), cache);
        return getCharIndexAtX(runGroups, maxWidth, -1);
    }

    // Gets the index of the last char that fits in a width of x, starting
    // from the char at index `from`.
    public int getCharIndexAtX(CaxtonText text, float x, int from) {
        Threshold threshold = new Threshold(from);
        for (RunGroup runGroup : text.runGroups()) {
            if (threshold.shouldSkip(runGroup)) {
                continue;
            }
            if (runGroup.getFont() == null) {
                MutableFloat cumulWidth = new MutableFloat(x);
                MutableInt theIndex = new MutableInt();
                boolean completed = runGroup.accept((index, style, codePoint) -> {
                    if (threshold.updateLegacy(index + runGroup.getCharOffset())) {
                        return true;
                    }
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
                        if (threshold.updateCaxton(runGroup, runIndex, shapingResult, i)) {
                            continue;
                        }
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

    // TODO: can we avoid repeated traversal if we have something like this?:
    // getOffsetAtIndex(text, i2) - getOffsetAtIndex(text, i1)
    public float getOffsetAtIndex(CaxtonText text, int textIndex) {
        float offset = 0.0f;
        for (RunGroup runGroup : text.runGroups()) {
            ConfiguredCaxtonFont font = runGroup.getFont();
            if (font == null) {
                MutableFloat mutableFloat = new MutableFloat(offset);
                boolean completed = runGroup.accept((index, style, codePoint) -> index + runGroup.getCharOffset() != textIndex);
                offset = mutableFloat.floatValue();
                if (!completed) return offset;
            } else {
                float scale = runGroup.getFont().getScale();
                ShapingResult[] shapingResults = runGroup.getShapingResults();
                int[] bidiRuns = runGroup.getBidiRuns();
                int advance = 0;

                int runIndex = 0;
                for (ShapingResult shapingResult : shapingResults) {
                    int start = bidiRuns[3 * runIndex];
                    int level = bidiRuns[3 * runIndex + 2];
                    for (int i = 0; i < shapingResult.numGlyphs(); ++i) {
                        int r0 = runGroup.getCharOffset() + start + shapingResult.clusterIndex(i);
                        int r1 = runGroup.getCharOffset() + start + shapingResult.clusterLimit(i);
                        if (r0 <= textIndex && textIndex < r1) {
                            float frac = ((float) (textIndex - r0)) / (r1 - r0);
                            if (level % 2 != 0) { // RTL correction
                                frac = 1 - frac;
                            }
                            return offset + scale * (advance + frac * shapingResult.advanceX(i));
                        }
                        advance += shapingResult.advanceX(i);
                    }
                    ++runIndex;
                }

                offset += advance * scale;
            }
        }

        return offset;
    }

    public FloatList getHighlightRanges(CaxtonText text, int startIndex, int endIndex) {
        if (endIndex < startIndex)
            throw new IllegalArgumentException("startIndex must be less than or equal to endIndex");
        Highlighter highlighter = new Highlighter(startIndex, endIndex);

        for (RunGroup runGroup : text.runGroups()) {
            ConfiguredCaxtonFont font = runGroup.getFont();
            if (font == null) {
                runGroup.accept((index, style, codePoint, rtl) -> {
                    int index2 = index + runGroup.getCharOffset();
                    highlighter.accept(index2, index2 + 1, rtl, getWidth(codePoint, style));
                    return true;
                });
            } else {
                float scale = runGroup.getFont().getScale();
                ShapingResult[] shapingResults = runGroup.getShapingResults();
                int[] bidiRuns = runGroup.getBidiRuns();

                int runIndex = 0;
                for (ShapingResult shapingResult : shapingResults) {
                    int start = bidiRuns[3 * runIndex];
                    boolean rtl = bidiRuns[3 * runIndex + 2] % 2 != 0;
                    for (int i = 0; i < shapingResult.numGlyphs(); ++i) {
                        int r0 = runGroup.getCharOffset() + start + shapingResult.clusterIndex(i);
                        int r1 = runGroup.getCharOffset() + start + shapingResult.clusterLimit(i);
                        highlighter.accept(r0, r1, rtl, scale * shapingResult.advanceX(i));
                    }
                    ++runIndex;
                }
            }
        }

        return highlighter.getRanges();
    }

    public void clearCaches() {
        cache.clear();
    }

    private static class Highlighter {
        private final int startIndex, endIndex;
        private final FloatList ranges = new FloatArrayList();
        private boolean wasRtl = false;
        private float offset = 0.0f;
        private float leftBound = Float.NaN;

        private Highlighter(int startIndex, int endIndex) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
//            System.out.printf("Highlighter(%d, %d)\n", startIndex, endIndex);
        }

        public void accept(int r0, int r1, boolean rtl, float glyphWidth) {
//            System.out.printf("accept(%d, %d, %b, %f) with leftBound = %f, offset = %f\n", r0, r1, rtl, glyphWidth, leftBound, offset);
            if (r1 <= r0) {
                return;
            }
            if (!Float.isNaN(leftBound)) {
                float rightBound = Float.NaN;
                if (rtl != wasRtl) {
                    rightBound = offset;
                } else if (rtl && r1 >= startIndex && startIndex > r0) {
                    float frac = ((float) startIndex - r1) / (r0 - r1);
                    rightBound = offset + frac * glyphWidth;
                } else if (!rtl && r0 <= endIndex && endIndex < r1) {
                    float frac = ((float) endIndex - r0) / (r1 - r0);
                    rightBound = offset + frac * glyphWidth;
                }
                if (!Float.isNaN(rightBound)) {
                    ranges.add(leftBound);
                    ranges.add(rightBound);
                    leftBound = Float.NaN;
                }
            }
            if (Float.isNaN(leftBound)) {
                if (rtl && r1 >= endIndex && endIndex > r0) {
                    float frac = ((float) endIndex - r1) / (r0 - r1);
                    leftBound = offset + frac * glyphWidth;
                } else if (!rtl && r0 <= startIndex && startIndex < r1) {
                    float frac = ((float) startIndex - r0) / (r1 - r0);
                    leftBound = offset + frac * glyphWidth;
                }
            }
            offset += glyphWidth;
            wasRtl = rtl;
        }

        public FloatList getRanges() {
            if (!Float.isNaN(leftBound)) {
                ranges.add(leftBound);
                ranges.add(offset);
                leftBound = Float.NaN;
            }
//            System.err.println(ranges);
            return ranges;
        }
    }
}
