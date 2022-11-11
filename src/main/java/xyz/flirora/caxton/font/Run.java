package xyz.flirora.caxton.font;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.FontStorage;
import net.minecraft.text.CharacterVisitor;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A run of characters with the same style.
 *
 * @param text  A @ref StringBuffer of the characters.
 * @param style The style shared by the characters.
 */
@Environment(EnvType.CLIENT)
public record Run(String text, Style style, @Nullable CaxtonFont font) {
    @NotNull
    public static List<RunGroup> splitIntoGroups(OrderedText text, Function<Identifier, FontStorage> fonts, boolean validateAdvance) {
        List<Run> runs = splitIntoRuns(text, fonts, validateAdvance);
        return groupCompatible(runs);
    }

    @NotNull
    private static List<Run> splitIntoRuns(OrderedText text, Function<Identifier, FontStorage> fonts, boolean validateAdvance) {
        RunLister lister = new RunLister(fonts, validateAdvance);
        text.accept(lister);
        return lister.getRuns();
    }

    @NotNull
    public static List<RunGroup> groupCompatible(List<Run> runs) {
        List<List<Run>> runGroups = new ArrayList<>();
        for (Run run : runs) {
            if (runGroups.isEmpty() ||
                    !areRunsCompatible(
                            runGroups.get(runGroups.size() - 1).get(0),
                            run)) {
                List<Run> group = new ArrayList<>();
                group.add(run);
                runGroups.add(group);
            } else {
                runGroups.get(runGroups.size() - 1).add(run);
            }
        }
        return runGroups.stream().map(RunGroup::new).collect(Collectors.toList());
    }

    private static boolean areRunsCompatible(Run a, Run b) {
        return a.font == b.font;
    }

    private static class PendingRun {
        private final StringBuffer contents;
        private final Style style;
        private final @Nullable CaxtonFont font;

        private PendingRun(StringBuffer contents, Style style, @Nullable CaxtonFont font) {
            this.contents = contents;
            this.style = style;
            this.font = font;
        }

        public void appendCodePoint(int codePoint) {
            contents.appendCodePoint(codePoint);
        }

        public Run bake() {
            return new Run(contents.toString(), style, font);
        }
    }

    private static class RunLister implements CharacterVisitor {
        private final List<PendingRun> runs;
        private final Function<Identifier, FontStorage> fonts;
        private final boolean validateAdvance;

        private RunLister(Function<Identifier, FontStorage> fonts, boolean validateAdvance) {
            this.runs = new ArrayList<>();
            this.fonts = fonts;
            this.validateAdvance = validateAdvance;
        }

        @Override
        public boolean accept(int index, Style style, int codePoint) {
            CaxtonFont font = ((CaxtonFontStorage) fonts.apply(style.getFont())).getCaxtonGlyph(codePoint, validateAdvance, style).getCaxtonFont();
            if (runs.isEmpty()) {
                addNewRun(codePoint, style, font);
            } else {
                PendingRun lastRun = runs.get(runs.size() - 1);
                if (lastRun.style.equals(style) && lastRun.font == font) {
                    lastRun.appendCodePoint(codePoint);
                } else {
                    addNewRun(codePoint, style, font);
                }
            }
            return true;
        }

        private void addNewRun(int codePoint, Style style, @Nullable CaxtonFont font) {
            runs.add(new PendingRun(new StringBuffer().appendCodePoint(codePoint), style, font));
        }

        public List<Run> getRuns() {
            return runs.stream().map(PendingRun::bake).collect(Collectors.toList());
        }
    }
}
