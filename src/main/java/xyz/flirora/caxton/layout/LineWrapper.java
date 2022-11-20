package xyz.flirora.caxton.layout;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.BreakIterator;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.TextHandler;
import net.minecraft.text.Style;
import net.minecraft.util.Identifier;
import xyz.flirora.caxton.font.ConfiguredCaxtonFont;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public class LineWrapper {
    private final CaxtonText text;
    private final BreakIterator bi;
    private final String contents;
    private final float maxWidth;
    private final float[] widths;
    // Invariant: If nextLineBreak() was called before, then currentLineStart is the result of the latest such call.
    private int currentLineStart = 0;
    private int rgIndex = 0;
    // Invariant: targetBreakPoint is the last value returned by bi.next().
    private int targetBreakPoint;
    private boolean continuation = false;

    public LineWrapper(CaxtonText text, TextHandler.WidthRetriever widthRetriever, float maxWidth) {
        this.text = text;
        this.maxWidth = maxWidth;
        this.bi = BreakIterator.getLineInstance(Locale.getDefault());
        this.contents = text.getContents();
        bi.setText(contents);
        this.targetBreakPoint = bi.next();

        this.widths = new float[text.totalLength()];
        Arrays.fill(widths, Float.NaN);

        List<RunGroup> groups = text.runGroups();
        for (RunGroup runGroup : groups) {
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
        }

//        System.err.println("Line-wrapping " + contents);
//        System.err.println("maxWidth = " + maxWidth);
//        System.err.println("text = " + text);
//        System.err.println("widths = " + Arrays.toString(widths));
    }

    public int getCurrentLineStart() {
        return currentLineStart;
    }

    public List<Run> nextLine(Function<Identifier, FontStorage> fontStorageAccessor) {
        int index = computeNextLineBreak();

        Run.RunLister lister = new Run.RunLister(fontStorageAccessor, false);
        int lastNonspace = index;
        while (lastNonspace > currentLineStart && UCharacter.isWhitespace(contents.charAt(lastNonspace - 1))) {
            --lastNonspace;
        }
        for (int i = currentLineStart; i < lastNonspace; ) {
            int codePoint = contents.codePointAt(i);
            RunGroup group;
            while (i >= (group = this.text.runGroups().get(rgIndex)).getCharOffset() + group.getTotalLength()) {
                ++rgIndex;
            }
            Style style = group.getStyleAt(i - group.getCharOffset());
            lister.accept(i - currentLineStart, style, codePoint);
            i += Character.charCount(codePoint);
        }

//        System.err.println("Line ended at " + index + "(" + lastNonspace + ")");
        currentLineStart = index;
        return lister.getRuns();
    }

    public void goToNextLine() {
        currentLineStart = computeNextLineBreak();
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
                    targetBreakPoint = bi.next();
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
                targetBreakPoint = bi.next();
            }
            prevIndex = index;
        }
        return index;
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
}
