package xyz.flirora.caxton.layout;

import com.ibm.icu.text.Bidi;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.FontStorage;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Represents text laid out by Caxton.
 *
 * @param runGroups The list of run groups in this text.
 */
@Environment(EnvType.CLIENT)
public record CaxtonText(List<RunGroup> runGroups, int totalLength, boolean rtl) {
    public CaxtonText(List<RunGroup> runGroups, boolean rtl) {
        this(runGroups, runGroups.stream().mapToInt(RunGroup::getTotalLength).sum(), rtl);
    }

    @NotNull
    public static CaxtonText from(OrderedText text, Function<Identifier, FontStorage> fonts, boolean validateAdvance, boolean rtl, LayoutCache cache) {
        List<Run> runs = Run.splitIntoRuns(text, fonts, validateAdvance);
        return groupCompatible(runs, rtl, cache);
    }

    @NotNull
    public static CaxtonText fromFormatted(StringVisitable text, Function<Identifier, FontStorage> fonts, Style style, boolean validateAdvance, boolean rtl, LayoutCache cache) {
        List<Run> runs = Run.splitIntoRunsFormatted(text, fonts, style, validateAdvance);
        return groupCompatible(runs, rtl, cache);
    }

    @NotNull
    public static CaxtonText fromForwards(StringVisitable text, Function<Identifier, FontStorage> fonts, Style style, boolean validateAdvance, boolean rtl, LayoutCache cache) {
        List<Run> runs = Run.splitIntoRunsForwards(text, fonts, style, validateAdvance);
        return groupCompatible(runs, rtl, cache);
    }

    @NotNull
    public static CaxtonText fromFormatted(String text, Function<Identifier, FontStorage> fonts, Style style, boolean validateAdvance, boolean rtl, LayoutCache cache) {
        List<Run> runs = Run.splitIntoRunsFormatted(text, fonts, style, validateAdvance);
        return groupCompatible(runs, rtl, cache);
    }

    @NotNull
    public static CaxtonText fromFormatted(String text, Function<Identifier, FontStorage> fonts, Style style, boolean validateAdvance, boolean rtl, LayoutCache cache, ForwardTraversedMap formattingCodeStarts) {
        List<Run> runs = Run.splitIntoRunsFormatted(text, fonts, style, validateAdvance, formattingCodeStarts);
        return groupCompatible(runs, rtl, cache);
    }

    @NotNull
    public static CaxtonText fromForwards(String text, Function<Identifier, FontStorage> fonts, Style style, boolean validateAdvance, boolean rtl, LayoutCache cache) {
        List<Run> runs = Run.splitIntoRunsForwards(text, fonts, style, validateAdvance);
        return groupCompatible(runs, rtl, cache);
    }

    @NotNull
    public static CaxtonText groupCompatible(List<Run> runs, boolean rtl, LayoutCache cache) {
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

        int charOffset = 0;

        // Construct RunGroup objects for each list of compatible runs.
        int currentBidiRun = 0;
        int currentBidiStringIndex = 0;
        int totalBidiRuns = bidi.countRuns();

//        System.out.println(runs.stream().map(Run::text).collect(Collectors.joining()));
        List<RunGroup> groups = new ArrayList<>();
//        if (totalBidiRuns > 1) {
//            System.err.println("Bidi results:");
//            for (int i = 0; i < totalBidiRuns; ++i) {
//                System.err.println(i + " => " + bidi.getRunStart(i) + " - " + bidi.getRunLimit(i) + " @ " + bidi.getRunLevel(i));
//            }
//        }
        for (List<Run> group : runGroups) {
            int firstBidiRunInGroup = currentBidiRun;
            int firstBidiStringIndex = currentBidiStringIndex;
            for (Run run : group) {
                currentBidiStringIndex += run.text().length();
            }
            // Advance to the bidi run for the end of the run group’s text
            while (currentBidiRun < totalBidiRuns && bidi.getRunStart(currentBidiRun) < currentBidiStringIndex) {
                ++currentBidiRun;
            }
//            if (totalBidiRuns > 1)
//                System.err.println("run group from " + firstBidiStringIndex + " to " + currentBidiStringIndex + ": firstBidiRunInGroup = " + firstBidiRunInGroup + ", firstBidiStringIndex = " + firstBidiStringIndex);
            if (currentBidiRun == totalBidiRuns || bidi.getRunStart(currentBidiRun) >= currentBidiStringIndex)
                --currentBidiRun;
            // Always compute bidi runs for this run group;
            // this information is useful for reshaping in legacy fonts.
            int[] bidiRuns = new int[3 * (currentBidiRun - firstBidiRunInGroup + 1)];
            for (int i = firstBidiRunInGroup; i <= currentBidiRun; ++i) {
//                if (totalBidiRuns > 1) {
//                    System.err.println("i = " + i);
//                    System.err.println("firstBidiStringIndex = " + firstBidiStringIndex);
//                    System.err.println("start = " + bidi.getRunStart(i));
//                    System.err.println("limit = " + bidi.getRunLimit(i));
//                    System.err.println("level = " + bidi.getRunLevel(i));
//                }
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
            int runLevel = bidi.getRunLevel(firstBidiRunInGroup);
//            System.out.println(group);
//            System.out.println(Arrays.toString(bidiRuns));
            RunGroup runGroup = new RunGroup(group, runLevel, charOffset, bidiRuns, cache);
            groups.add(runGroup);

            charOffset += runGroup.getTotalLength();

            if (currentBidiRun < totalBidiRuns && currentBidiStringIndex >= bidi.getRunLimit(currentBidiRun))
                ++currentBidiRun;
        }
        return new CaxtonText(reorderRunGroups(groups), bidi.getParaLevel() % 2 != 0);
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
        return a.font() == b.font();
    }

    public String getContents() {
        StringBuilder buffer = new StringBuilder();
        for (RunGroup runGroup : runGroups) {
            buffer.append(runGroup.getJoined());
        }
        return buffer.toString();
    }
}
