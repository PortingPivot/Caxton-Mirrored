package xyz.flirora.caxton.font;

import net.minecraft.text.CharacterVisitor;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A run of characters with the same style.
 *
 * @param text  A @ref StringBuffer of the characters.
 * @param style The style shared by the characters.
 */
public record Run(StringBuffer text, Style style) {
    @NotNull
    public static List<List<Run>> splitIntoGroups(OrderedText text) {
        List<Run> runs = splitIntoRuns(text);
        return groupCompatible(runs);
    }

    @NotNull
    private static List<Run> splitIntoRuns(OrderedText text) {
        List<Run> runs = new ArrayList<>();
        text.accept(new RunLister(runs));
        return runs;
    }

    @NotNull
    public static List<List<Run>> groupCompatible(List<Run> runs) {
        List<List<Run>> runGroups = new ArrayList<>();
        for (Run run : runs) {
            if (runGroups.isEmpty() ||
                    !areStylesCompatible(
                            runGroups.get(runGroups.size() - 1).get(0).style(),
                            run.style())) {
                List<Run> group = new ArrayList<>();
                group.add(run);
                runGroups.add(group);
            } else {
                runGroups.get(runGroups.size() - 1).add(run);
            }
        }
        return runGroups;
    }

    private static boolean areStylesCompatible(Style a, Style b) {
        return a.isBold() == b.isBold() && a.isItalic() == b.isItalic() && a.isObfuscated() == b.isObfuscated() && a.getFont().equals(b.getFont());
    }

    private static class RunLister implements CharacterVisitor {
        private final List<Run> runs;

        private RunLister(List<Run> runs) {
            this.runs = runs;
        }

        @Override
        public boolean accept(int index, Style style, int codePoint) {
            if (runs.isEmpty()) {
                runs.add(new Run(new StringBuffer().appendCodePoint(codePoint), style));
            } else {
                Run lastRun = runs.get(runs.size() - 1);
                if (lastRun.style().equals(style)) {
                    lastRun.text().appendCodePoint(codePoint);
                } else {
                    runs.add(new Run(new StringBuffer().appendCodePoint(codePoint), style));
                }
            }
            return true;
        }
    }
}
