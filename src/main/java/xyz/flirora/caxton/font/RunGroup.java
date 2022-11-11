package xyz.flirora.caxton.font;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A group of runs that can be shaped together, even if they have different
 * styles.
 * <p>
 * This requires all runs to use the same font.
 */
public class RunGroup {
    private final List<Run> runs;
    private final String joined;

    public RunGroup(List<Run> runs) {
        // BreakIterator breakIterator = BreakIterator.getLineInstance();
        if (runs.isEmpty()) {
            throw new IllegalArgumentException("runs may not be empty");
        }
        this.runs = runs;
        this.joined = runs.stream().map(Run::text).collect(Collectors.joining());
    }

    public @Nullable CaxtonFont getFont() {
        return runs.get(0).font();
    }

    public String toString() {
        return "RunGroup[runs=" + runs + "]";
    }
}
