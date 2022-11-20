package xyz.flirora.caxton.layout;

import com.ibm.icu.lang.UCharacter;
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
import xyz.flirora.caxton.font.ConfiguredCaxtonFont;
import xyz.flirora.caxton.mixin.TextHandlerAccessor;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
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

    // Gets the index of the first char that fails to fit in a width of x, starting from the char at index `from`.
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
                    int index2 = index + runGroup.getCharOffset();
                    if (threshold.updateLegacy(index2)) {
                        return true;
                    }
                    float width = getWidth(codePoint, style);
                    if (cumulWidth.floatValue() < width) {
                        theIndex.setValue(index2);
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

    // Gets the index of the first char that starts past a width of x.
    public int getCharIndexAfterX(CaxtonText text, float x, int from) {
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
                    if (cumulWidth.floatValue() < 0) {
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
                        if (x < 0) {
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


    /**
     * Given the index of a char in a piece of text, return its horizontal position.
     *
     * @param text      The {@link CaxtonText} to use.
     * @param textIndex The UTF-16 code unit index to get the position for.
     * @param direction If this is {@link DirectionSetting#AUTO}, then gets the position corresponding to the startward edge of the glyph, accounting for bidirectional text. Otherwise, always gets either the left (for {@link DirectionSetting#FORCE_LTR}) or the right (for {@link DirectionSetting#FORCE_RTL}) edge of the glyph.
     * @return An x-offset from the left edge of the text.
     */
    // TODO: can we avoid repeated traversal if we have something like this?:
    // getOffsetAtIndex(text, i2) - getOffsetAtIndex(text, i1)
    public float getOffsetAtIndex(CaxtonText text, int textIndex, DirectionSetting direction) {
        if (direction.treatAsRtl(text.rtl()) ?
                textIndex >= text.totalLength() :
                textIndex < 0) {
            return 0.0f;
        }
        float offset = 0.0f;
        for (RunGroup runGroup : text.runGroups()) {
            ConfiguredCaxtonFont font = runGroup.getFont();
            if (font == null) {
                MutableFloat mutableFloat = new MutableFloat(offset);
                boolean completed = runGroup.accept((index, style, codePoint, rtl) -> {
                    if (index + runGroup.getCharOffset() == textIndex) {
                        if (direction.treatAsRtl(rtl))
                            mutableFloat.add(getWidth(codePoint, style));
                        return false;
                    }
                    mutableFloat.add(getWidth(codePoint, style));
                    return true;
                });
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
                            if (direction.treatAsRtl(level % 2 != 0)) { // RTL correction
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

    public void getHighlightRanges(CaxtonText text, int startIndex, int endIndex, HighlightConsumer callback) {
        if (endIndex < startIndex)
            throw new IllegalArgumentException("startIndex must be less than or equal to endIndex");
        Highlighter highlighter = new Highlighter(startIndex, endIndex, callback);

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

        highlighter.finish();
    }

    public void wrapLines(String text, int maxWidth, Style style, boolean retainTrailingWordSplit, TextHandler.LineWrappingConsumer consumer) {
        // Apparently, vanilla uses TextVisitFactory.visitFormatted for this.
        ForwardTraversedMap formattingCodeStarts = new ForwardTraversedMap();
        CaxtonText caxtonText = CaxtonText.fromFormatted(text, fontStorageAccessor, style, false, false, cache, formattingCodeStarts);
        wrapLines(caxtonText, maxWidth, consumer, formattingCodeStarts, retainTrailingWordSplit);
    }

    public void wrapLines(
            CaxtonText text,
            int maxWidth,
            TextHandler.LineWrappingConsumer lineConsumer,
            ForwardTraversedMap formattingCodeStarts,
            boolean retainTrailingWordSplit) {
        // lineConsumer: (visual line, is continuation)
        int rgIndex = 0;
        LineWrapper wrapper = new LineWrapper(
                text,
                ((TextHandlerAccessor) vanillaHandler).getWidthRetriever(),
                maxWidth);
        String contents = wrapper.getContents();
        while (!wrapper.isFinished()) {
            int start = wrapper.getCurrentLineStart();
            wrapper.goToNextLine();
            int end = wrapper.getCurrentLineStart();
            if (!retainTrailingWordSplit) {
                while (end > 0 && UCharacter.isWhitespace(contents.charAt(end - 1))) {
                    --end;
                }
            }
            RunGroup rg;
            while (start >= (rg = text.runGroups().get(rgIndex)).getCharOffset() + rg.getTotalLength()) {
                ++rgIndex;
            }
            lineConsumer.accept(
                    rg.getStyleAt(start - rg.getCharOffset()),
                    offsetForFormattingCodes(start, formattingCodeStarts),
                    offsetForFormattingCodes(end, formattingCodeStarts));
        }
    }

    private int offsetForFormattingCodes(int index, ForwardTraversedMap formattingCodeStarts) {
        return index + 2 * formattingCodeStarts.inf(index);
    }

    public void wrapLines(StringVisitable text, int maxWidth, Style style, BiConsumer<StringVisitable, Boolean> lineConsumer) {
        // Apparently, vanilla uses TextVisitFactory.visitFormatted for this.
        CaxtonText caxtonText = CaxtonText.fromFormatted(text, fontStorageAccessor, style, false, false, cache);
        wrapLines(caxtonText, maxWidth, lineConsumer);
    }

    public void wrapLines(CaxtonText text, int maxWidth, BiConsumer<StringVisitable, Boolean> lineConsumer) {
        // lineConsumer: (visual line, is continuation)
        LineWrapper wrapper = new LineWrapper(
                text,
                ((TextHandlerAccessor) vanillaHandler).getWidthRetriever(),
                maxWidth);
        while (!wrapper.isFinished()) {
            boolean continuation = wrapper.isContinuation();
            List<Run> line = wrapper.nextLine(fontStorageAccessor);
            lineConsumer.accept(runsToStringVisitable(line), continuation);
        }
    }

    private StringVisitable runsToStringVisitable(List<Run> runs) {
        return new StringVisitable() {
            @Override
            public <T> Optional<T> visit(Visitor<T> visitor) {
                for (Run run : runs) {
                    var result = visitor.accept(run.text());
                    if (result.isPresent()) return result;
                }
                return Optional.empty();
            }

            @Override
            public <T> Optional<T> visit(StyledVisitor<T> styledVisitor, Style style) {
                for (Run run : runs) {
                    var result = styledVisitor.accept(run.style().withParent(style), run.text());
                    if (result.isPresent()) return result;
                }
                return Optional.empty();
            }
        };
    }

    public void clearCaches() {
        cache.clear();
    }

    @FunctionalInterface
    public interface HighlightConsumer {
        void accept(float left, float right);
    }

    private static class Highlighter {
        private final int startIndex, endIndex;
        private final HighlightConsumer callback;
        private boolean wasRtl = false;
        private float offset = 0.0f;
        private float leftBound = Float.NaN;

        private Highlighter(int startIndex, int endIndex, HighlightConsumer callback) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.callback = callback;
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
                } else if (r1 < startIndex || endIndex < r0) {
                    rightBound = offset;
                }
                if (!Float.isNaN(rightBound)) {
                    callback.accept(leftBound, rightBound);
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
                } else if (startIndex <= r0 && r1 <= endIndex) {
                    leftBound = offset;
                }
            }
            offset += glyphWidth;
            wasRtl = rtl;
        }

        public void finish() {
            if (!Float.isNaN(leftBound)) {
                callback.accept(leftBound, offset);
                leftBound = Float.NaN;
            }
        }
    }
}
