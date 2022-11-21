package xyz.flirora.caxton.layout;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Bidi;
import com.ibm.icu.text.BreakIterator;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.TextHandler;
import net.minecraft.text.Style;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import xyz.flirora.caxton.font.ConfiguredCaxtonFont;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public class LineWrapper {
    private final CaxtonText text;
    private final Bidi bidi;
    private final @Nullable BreakIterator bi;
    private final String contents;
    private final float maxWidth;
    private final float[] widths;
    private final ForwardTraversedMap runGroupIndicesByStart;
    // Invariant: If nextLineBreak() was called before, then currentLineStart is the result of the latest such call.
    private int currentLineStart = 0;
    private int brIndex = 0;
    // Invariant: targetBreakPoint is the last value returned by bi.next().
    private int targetBreakPoint;
    private boolean continuation = false;

    public LineWrapper(CaxtonText text, Bidi bidi, TextHandler.WidthRetriever widthRetriever, float maxWidth, boolean breakAnywhere) {
        this.text = text;
        this.bidi = bidi;
        this.maxWidth = maxWidth;
        this.contents = text.getContents();
        if (breakAnywhere) {
            this.bi = null;
        } else {
            this.bi = BreakIterator.getLineInstance(Locale.getDefault());
            bi.setText(contents);
        }
        this.targetBreakPoint = nextEligibleBreakPoint();

        this.widths = new float[text.totalLength()];
        Arrays.fill(widths, Float.NaN);

        List<RunGroup> groups = text.runGroups();
        LongList packedRunGroupStartIndexPairs = new LongArrayList();
        for (int k = 0; k < groups.size(); k++) {
            RunGroup runGroup = groups.get(k);
            ConfiguredCaxtonFont font = runGroup.getFont();
            if (font == null) {
                // Legacy run
                runGroup.accept((index, style, codePoint, rtl) -> {
                    float width = widthRetriever.getWidth(codePoint, style);
                    int index2 = index + runGroup.getCharOffset();
                    widths[index2] = width;
                    return true;
                });
            } else {
                int[] bidiRuns = runGroup.getBidiRuns();
                ShapingResult[] shapingResults = runGroup.getShapingResults();
                for (int j = 0; j < shapingResults.length; j++) {
                    ShapingResult shapingResult = shapingResults[j];
                    int runStart = bidiRuns[3 * j];
                    for (int i = 0; i < shapingResult.numGlyphs(); ++i) {
                        int index = shapingResult.clusterIndex(i);
                        int advance = shapingResult.advanceX(i);
                        float width = font.getScale() * advance;
                        int index2 = index + runStart + runGroup.getCharOffset();
                        widths[index2] = width;
                    }
                }
            }
            packedRunGroupStartIndexPairs.add((((long) k) << 32) | runGroup.getCharOffset());
        }
        packedRunGroupStartIndexPairs.sort((a, b) -> Integer.compare((int) a, (int) b));
        runGroupIndicesByStart = new ForwardTraversedMap();
        for (long x : packedRunGroupStartIndexPairs) {
            runGroupIndicesByStart.put((int) x, (int) (x >> 32));
        }

//        System.err.println("Line-wrapping " + contents);
//        System.err.println("maxWidth = " + maxWidth);
//        System.err.println("text = " + text);
//        System.err.println("widths = " + Arrays.toString(widths));
    }

    public int getCurrentLineStart() {
        return currentLineStart;
    }

    public Result nextLine(Function<Identifier, FontStorage> fontStorageAccessor) {
        boolean rtl = isCurrentlyRtl();
        int index = computeNextLineBreak();

        Run.RunLister lister = new Run.RunLister(fontStorageAccessor, false);
        int lastNonspace = index;
        while (lastNonspace > currentLineStart && UCharacter.isWhitespace(contents.charAt(lastNonspace - 1))) {
            --lastNonspace;
        }
        for (int i = currentLineStart; i < lastNonspace; ) {
            int codePoint = contents.codePointAt(i);
            RunGroup group = getRunGroupAt(i);
            Style style = group.getStyleAt(i - group.getCharOffset());
            lister.accept(i - currentLineStart, style, codePoint);
            i += Character.charCount(codePoint);
        }

//        System.err.println("Line ended at " + index + "(" + lastNonspace + ")");
        currentLineStart = index;
        return new Result(lister.getRuns(), rtl);
    }

    public void goToNextLine() {
        currentLineStart = computeNextLineBreak();
    }

    public boolean isCurrentlyRtl() {
        while (brIndex < bidi.getRunCount() - 1 && currentLineStart >= bidi.getRunLimit(brIndex))
            ++brIndex;
        return bidi.getRunLevel(brIndex) % 2 != 0;
    }

    public RunGroup getRunGroupAt(int index) {
        int k = runGroupIndicesByStart.inf(index + 1);
        return text.runGroups().get(k);
    }

    private int computeNextLineBreak() {
        //        System.err.println("nextLine: currentLineStart = " + currentLineStart);
        float remainingLineWidth = maxWidth;
        int prevBreakPoint = -1;
        int index;
        int prevIndex = -1;
        for (index = currentLineStart; index < contents.length(); index = nextIndex(index)) {
//            System.err.println(index + " " + remainingLineWidth + " " + widths[index]);
            if (contents.charAt(index) == '\n') {
                continuation = false;
                ++index;
                if (index >= targetBreakPoint) {
                    targetBreakPoint = nextEligibleBreakPoint();
                }
                break;
            }
            remainingLineWidth -= widths[index];
            if (remainingLineWidth < 0.0f) {
                // Drat, we’re out of room!
                index = prevBreakPoint == -1 ? prevIndex : prevBreakPoint;
                break;
            }
            if (index >= targetBreakPoint) {
                prevBreakPoint = targetBreakPoint;
                targetBreakPoint = nextEligibleBreakPoint();
            }
            prevIndex = index;
        }
        return index;
    }

    private int nextEligibleBreakPoint() {
        if (bi == null) return targetBreakPoint + 1;
        return bi.next();
    }

    // NB: this gives the result for the next `nextLineBreak()` call, not the previous one
    public boolean isContinuation() {
        return continuation;
    }

    public String getContents() {
        return contents;
    }

    public boolean isFinished() {
        return currentLineStart >= contents.length();
    }

    private int nextIndex(int i) {
        ++i;
        while (i < contents.length() && Float.isNaN(widths[i])) ++i;
        return i;
    }

    public record Result(List<Run> runs, boolean rtl) {
    }
}
