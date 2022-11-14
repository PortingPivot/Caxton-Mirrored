package xyz.flirora.caxton.font;

import com.ibm.icu.text.Bidi;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.FontStorage;
import net.minecraft.text.*;
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
public record Run(String text, Style style, @Nullable ConfiguredCaxtonFont font) {
    @NotNull
    public static List<RunGroup> splitIntoGroups(OrderedText text, Function<Identifier, FontStorage> fonts, boolean validateAdvance, boolean rtl) {
        List<Run> runs = splitIntoRuns(text, fonts, validateAdvance, rtl);
        return groupCompatible(runs, rtl);
    }

    @NotNull
    public static List<RunGroup> splitIntoGroups(StringVisitable text, Function<Identifier, FontStorage> fonts, boolean validateAdvance, boolean rtl) {
        List<Run> runs = splitIntoRuns(text, fonts, validateAdvance, rtl);
        return groupCompatible(runs, rtl);
    }

    @NotNull
    public static List<RunGroup> splitIntoGroups(String text, Function<Identifier, FontStorage> fonts, boolean validateAdvance, boolean rtl) {
        List<Run> runs = splitIntoRuns(text, fonts, validateAdvance, rtl);
        return groupCompatible(runs, rtl);
    }

    @NotNull
    private static List<Run> splitIntoRuns(OrderedText text, Function<Identifier, FontStorage> fonts, boolean validateAdvance, boolean rtl) {
        RunLister lister = new RunLister(fonts, validateAdvance, rtl);
        text.accept(lister);
        return lister.getRuns();
    }

    @NotNull
    private static List<Run> splitIntoRuns(StringVisitable text, Function<Identifier, FontStorage> fonts, boolean validateAdvance, boolean rtl) {
        RunLister lister = new RunLister(fonts, validateAdvance, rtl);
        TextVisitFactory.visitFormatted(text, Style.EMPTY, lister);
        return lister.getRuns();
    }

    @NotNull
    private static List<Run> splitIntoRuns(String text, Function<Identifier, FontStorage> fonts, boolean validateAdvance, boolean rtl) {
        RunLister lister = new RunLister(fonts, validateAdvance, rtl);
        TextVisitFactory.visitFormatted(text, Style.EMPTY, lister);
        return lister.getRuns();
    }

    @NotNull
    public static List<RunGroup> groupCompatible(List<Run> runs, boolean rtl) {
        // Perform bidi analysis on the entire string.
        Bidi bidi = new Bidi(
                runs.stream().map(Run::text).collect(Collectors.joining()),
                rtl ? Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT
                        : Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);

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

        // Construct RunGroup objects for each list of compatible runs.
        int currentBidiRun = 0;
        int currentBidiStringIndex = 0;
        int totalBidiRuns = bidi.countRuns();

//        System.out.println(runs.stream().map(Run::text).collect(Collectors.joining()));
        List<RunGroup> groups = new ArrayList<>();
        for (List<Run> group : runGroups) {
            int firstBidiRunInGroup = currentBidiRun;
            int firstBidiStringIndex = currentBidiStringIndex;
            for (Run run : group) {
                // Advance to the bidi run for the end of the style run’s text
                currentBidiStringIndex += run.text.length();
                while (currentBidiRun < totalBidiRuns && bidi.getRunStart(currentBidiRun) < currentBidiStringIndex) {
                    ++currentBidiRun;
                }
            }
            if (currentBidiRun == totalBidiRuns || bidi.getRunStart(currentBidiRun) == currentBidiStringIndex)
                --currentBidiRun;
            int[] bidiRuns = null;
            String reordered = null;
            if (group.get(0).font() != null) {
                bidiRuns = new int[3 * (currentBidiRun - firstBidiRunInGroup + 1)];
                for (int i = firstBidiRunInGroup; i <= currentBidiRun; ++i) {
                    int j = i - firstBidiRunInGroup;
                    bidiRuns[3 * j] = Math.max(
                            0,
                            bidi.getRunStart(i) - firstBidiStringIndex);
                    bidiRuns[3 * j + 1] = Math.min(
                            currentBidiStringIndex - firstBidiStringIndex,
                            bidi.getRunLimit(i) - firstBidiStringIndex);
                    bidiRuns[3 * j + 2] = bidi.getRunLevel(i);
                }
                bidiRuns = reorderBidiRuns(bidiRuns);
            }
            int runLevel = bidi.getRunLevel(firstBidiRunInGroup);
//            System.out.println(group);
//            System.out.println(Arrays.toString(bidiRuns));
            groups.add(new RunGroup(group, runLevel, bidiRuns));
        }
        return reorderRunGroups(groups);
    }

    private static int[] reorderBidiRuns(int[] runs) {
        int nRuns = runs.length / 3;
        if (nRuns == 0) return runs;

        byte[] levels = new byte[nRuns];
        for (int i = 0; i < nRuns; ++i) {
            levels[i] = (byte) runs[3 * i + 2];
        }
        int[] indices = Bidi.reorderVisual(levels);

        int[] runsOut = new int[runs.length];

        for (int i = 0; i < nRuns; ++i) {
            int j = indices[i];
            runsOut[3 * i] = runs[3 * j];
            runsOut[3 * i + 1] = runs[3 * j + 1];
            runsOut[3 * i + 2] = runs[3 * j + 2];
        }

        return runsOut;
    }

    private static List<RunGroup> reorderRunGroups(List<RunGroup> runGroups) {
        int nRuns = runGroups.size();

        byte[] levels = new byte[nRuns];
        for (int i = 0; i < nRuns; ++i) {
            levels[i] = (byte) runGroups.get(i).getRunLevel();
        }
        RunGroup[] runGroupsArray = runGroups.toArray(new RunGroup[0]);
        Bidi.reorderVisually(levels, 0, runGroupsArray, 0, nRuns);

        return List.of(runGroupsArray);
    }

    private static boolean areRunsCompatible(Run a, Run b) {
        return a.font == b.font;
    }

    private static class PendingRun {
        private final StringBuffer contents;
        private final Style style;
        private final @Nullable ConfiguredCaxtonFont font;

        private PendingRun(StringBuffer contents, Style style, @Nullable ConfiguredCaxtonFont font) {
            this.contents = contents;
            this.style = style;
            this.font = font;
        }

        public void appendCodePoint(int codePoint) {
            contents.appendCodePoint(codePoint);
        }

        public Run bake(boolean rtl) {
            String text = contents.toString();

            if (font == null) {
                text = RunGroup.reorderLegacy(text, rtl);
            }

            return new Run(text, style, font);
        }
    }

    private static class RunLister implements CharacterVisitor {
        private final List<PendingRun> runs;
        private final Function<Identifier, FontStorage> fonts;
        private final boolean validateAdvance;
        private final boolean rtl;

        private RunLister(Function<Identifier, FontStorage> fonts, boolean validateAdvance, boolean rtl) {
            this.rtl = rtl;
            this.runs = new ArrayList<>();
            this.fonts = fonts;
            this.validateAdvance = validateAdvance;
        }

        @Override
        public boolean accept(int index, Style style, int codePoint) {
            ConfiguredCaxtonFont font = ((CaxtonFontStorage) fonts.apply(style.getFont())).getCaxtonGlyph(codePoint, validateAdvance, style).getCaxtonFont();
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

        private void addNewRun(int codePoint, Style style, @Nullable ConfiguredCaxtonFont font) {
            runs.add(new PendingRun(new StringBuffer().appendCodePoint(codePoint), style, font));
        }

        public List<Run> getRuns() {
            return runs.stream().map(pr -> pr.bake(rtl)).collect(Collectors.toList());
        }
    }
}
