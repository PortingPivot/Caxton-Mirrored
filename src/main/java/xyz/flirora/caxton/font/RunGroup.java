package xyz.flirora.caxton.font;

import com.ibm.icu.text.Bidi;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A group of runs that can be shaped together, even if they have different
 * styles.
 * <p>
 * This requires all runs to use the same font.
 */
public class RunGroup {
    private static final boolean DEBUG = true;
    private final List<Run> runs;
    private final char[] joined;
    // The fields below are null when getFont() is null.
    private final int @Nullable [] bidiRuns;

    public RunGroup(List<Run> runs) {
        // BreakIterator breakIterator = BreakIterator.getLineInstance();
        if (runs.isEmpty()) {
            throw new IllegalArgumentException("runs may not be empty");
        }
        this.runs = runs;
        String joined = runs.stream().map(Run::text).collect(Collectors.joining());

        if (!DEBUG && runs.get(0).font() == null) {
            // Legacy font; don’t compute bidi info.
            this.bidiRuns = null;
        } else {
            // Caxton font; do compute bidi info.
            Bidi bidi = new Bidi(joined, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
            System.out.println(runs);
            int numberOfRuns = bidi.countRuns();
            this.bidiRuns = new int[2 * numberOfRuns];
            for (int i = 0; i < numberOfRuns; ++i) {
                this.bidiRuns[2 * i] = bidi.getRunStart(i);
                this.bidiRuns[2 * i + 1] = bidi.getRunLimit(i);
            }
        }

        this.joined = joined.toCharArray();
    }

    public @Nullable CaxtonFont getFont() {
        return runs.get(0).font();
    }

    public String toString() {
        return "RunGroup[runs=" + runs + ", bidiRuns=" + Arrays.toString(bidiRuns) + "]";
    }

    public List<Run> getRuns() {
        return runs;
    }

    public int @Nullable [] getBidiRuns() {
        return bidiRuns;
    }

    public char[] getJoined() {
        return joined;
    }
}
