package xyz.flirora.caxton.font;

import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.ArabicShapingException;
import com.ibm.icu.text.Bidi;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Style;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A group of runs that can be shaped together, even if they have different
 * styles.
 * <p>
 * A run group encompasses two distinct concepts of runs: <i>bidi runs</i>,
 * which are substrings whose characters share the same text direction, and
 * <i>style runs</i>, whose characters share the same style.
 * <p>
 * Runs in a run group must either all use the same Caxton font or all use
 * legacy fonts.
 */
@Environment(EnvType.CLIENT)
public class RunGroup {
    // A list of the runs in this group.
    // The runs are in visual order relative to each other, but the characters
    // within the run are logically ordered and not shaped.
    private final List<Run> styleRuns;
    private final char[] joined;
    private final int runLevel;
    // The character offset of this run group relative to all other run groups
    // in the text on a logical level; i.e. the total length in chars of all
    // run groups that appear logically before this one.
    private final int charOffset;
    // The bidirectional runs of the text, in visual left-to-right order.
    private final int[] bidiRuns;

    // The fields below are null when getFont() is *not* null.
    // The visually reshaped style runs.
    private final @Nullable List<Run> visualText;

    // The fields below are null when getFont() is null.
    // The codepoint offset at which each style run starts
    private final int @Nullable [] styleRunStarts;
    // Cached results for getting the style run associated with a string index,
    // optimized for sequential access.
    private int lastQueriedStylePosition = 0, lastQueriedStyleResult = 0;

    public RunGroup(List<Run> styleRuns, int runLevel, int charOffset, int[] bidiRuns) {
        this.runLevel = runLevel;
        this.charOffset = charOffset;
        // BreakIterator breakIterator = BreakIterator.getLineInstance();
        if (styleRuns.isEmpty()) {
            throw new IllegalArgumentException("runs may not be empty");
        }
        this.styleRuns = styleRuns;
        String joined = styleRuns.stream().map(Run::text).collect(Collectors.joining());
        this.joined = joined.toCharArray();

        this.bidiRuns = bidiRuns;

        this.styleRunStarts = new int[styleRuns.size() + 1];
        int x = 0;
        for (int i = 0; i < styleRuns.size(); ++i) {
            this.styleRunStarts[i] = x;
            x += styleRuns.get(i).text().length();
        }
        this.styleRunStarts[styleRuns.size()] = x;

        if (styleRuns.get(0).font() == null) {
            this.visualText = reorderLegacy(joined);
        } else {
            this.visualText = null;
        }
    }

    private List<Run> reorderLegacy(String joinedString) {
        ArabicShaping shaper = new ArabicShaping(ArabicShaping.LETTERS_SHAPE | ArabicShaping.TEXT_DIRECTION_VISUAL_LTR);
        List<Run> visualStyleRuns = new ArrayList<>();
        for (int i = 0; i < bidiRuns.length / 3; ++i) {
            int start = bidiRuns[3 * i];
            int end = bidiRuns[3 * i + 1];
            int level = bidiRuns[3 * i + 2];
            if (level % 2 != 0) {
                // RTL
                int si = end, sj = end;
                Style st = null;
                while (si >= start) {
                    Style ss = null;
                    if (si == start || (ss = getStyleAt(si - 1)) != st) {
                        if (st != null) {
                            String shapedText = Bidi.writeReverse(joinedString.substring(si, sj), Bidi.DO_MIRRORING);
                            try {
                                shapedText = shaper.shape(shapedText);
                            } catch (ArabicShapingException ignored) {
                            }
                            visualStyleRuns.add(new Run(
                                    shapedText,
                                    st,
                                    getFontAt(si)));
                        }
                        st = ss;
                        sj = si;
                    }
                    --si;
                }
            } else {
                // LTR
                int si = start, sj = start;
                Style st = null;
                while (si <= end) {
                    Style ss = null;
                    if (si == end || (ss = getStyleAt(si)) != st) {
                        if (st != null) {
                            visualStyleRuns.add(new Run(
                                    joinedString.substring(sj, si),
                                    st,
                                    getFontAt(si - 1)));
                        }
                        st = ss;
                        sj = si;
                    }
                    ++si;
                }
            }
        }
        return visualStyleRuns;
    }

    public @Nullable ConfiguredCaxtonFont getFont() {
        return styleRuns.get(0).font();
    }

    public String toString() {
        return "RunGroup[runs=" + styleRuns + ", bidiRuns=" + Arrays.toString(bidiRuns) + ", styleRunStarts=" + Arrays.toString(styleRunStarts) + ", visualText=" + visualText + ", charOffset=" + charOffset + ", runLevel=" + runLevel + ", #=" + joined.length + "]";
    }

    /**
     * @return the list of style runs, in logical order.
     * @deprecated This is probably not what you want. For rendering legacy-font text, use {@link RunGroup#getVisualText() instead.}
     */
    @Deprecated(forRemoval = false)
    public List<Run> getStyleRuns() {
        return styleRuns;
    }

    public int[] getBidiRuns() {
        return bidiRuns;
    }

    public char[] getJoined() {
        return joined;
    }

    public int getRunLevel() {
        return runLevel;
    }

    public int getCharOffset() {
        return charOffset;
    }

    public int getTotalLength() {
        return joined.length;
    }

    public @Nullable List<Run> getVisualText() {
        return visualText;
    }

    public Style getStyleAt(int index) {
        int result = getStyleIndexAt(index);
        return styleRuns.get(result).style();
    }

    public ConfiguredCaxtonFont getFontAt(int index) {
        int result = getStyleIndexAt(index);
        return styleRuns.get(result).font();
    }

    private int getStyleIndexAt(int index) {
        if (index < 0 || index >= this.joined.length) {
            throw new IndexOutOfBoundsException("index must be in [0, " + this.joined.length + "); got " + index);
        }
        int result = computeStyleIndexAt(index);
        lastQueriedStylePosition = index;
        lastQueriedStyleResult = result;
        return result;
    }

    private int computeStyleIndexAt(int index) {
        Objects.requireNonNull(styleRunStarts, "this method is not supported for legacy-font runs");
        if (index == lastQueriedStylePosition) return lastQueriedStyleResult;
        if (index == lastQueriedStylePosition + 1) {
            if (index >= styleRunStarts[lastQueriedStyleResult + 1]) {
                return lastQueriedStyleResult + 1;
            }
            return lastQueriedStyleResult;
        }
        if (index == lastQueriedStylePosition - 1) {
            if (lastQueriedStylePosition == styleRunStarts[lastQueriedStyleResult]) {
                return lastQueriedStyleResult - 1;
            }
            return lastQueriedStyleResult;
        }
        int result = Arrays.binarySearch(styleRunStarts, index);
        return result >= 0 ? result : -result - 2;
    }

    /**
     * Shape each bidi run of this run group, using a cache.
     *
     * @param shapingCache a shaping cache
     * @return an array of {@link ShapingResult}s for each bidi run
     */
    public ShapingResult[] shape(Map<ConfiguredCaxtonFont, Map<ShapedString, ShapingResult>> shapingCache) {
        ConfiguredCaxtonFont font = this.getFont();

        if (font == null) {
            throw new UnsupportedOperationException("shapeRunGroup requires a Caxton font (got a legacy font)");
        }

        var shapingCacheForFont = shapingCache.computeIfAbsent(font, f -> new HashMap<>());

        // Determine which runs need to be shaped
        int[] bidiRuns = this.getBidiRuns();
        IntList uncachedBidiRuns = new IntArrayList(bidiRuns.length);
        ShapingResult[] shapingResults = new ShapingResult[bidiRuns.length / 3];
        for (int i = 0; i < bidiRuns.length / 3; ++i) {
            int start = bidiRuns[3 * i];
            int end = bidiRuns[3 * i + 1];
            int level = bidiRuns[3 * i + 2];
            String s = new String(this.getJoined(), start, end - start);
            ShapedString key = new ShapedString(s, level % 2 != 0);
            ShapingResult sr = shapingCacheForFont.get(key);
            if (sr != null) {
                shapingResults[i] = sr;
            } else {
                uncachedBidiRuns.add(start);
                uncachedBidiRuns.add(end);
                uncachedBidiRuns.add(level);
            }
        }

        if (!uncachedBidiRuns.isEmpty()) {
            ShapingResult[] newlyComputed = font.shape(this.getJoined(), uncachedBidiRuns.toIntArray());

            // Fill in blanks from before
            for (int i = 0, j = 0; i < bidiRuns.length / 3; ++i) {
                if (shapingResults[i] == null) {
                    shapingResults[i] = newlyComputed[j];

                    int start = bidiRuns[3 * i];
                    int end = bidiRuns[3 * i + 1];
                    int level = bidiRuns[3 * i + 2];
                    String s = new String(this.getJoined(), start, end - start);
                    ShapedString key = new ShapedString(s, level % 2 != 0);
                    shapingCacheForFont.put(key, newlyComputed[j]);

                    ++j;
                }
            }
        }

        return shapingResults;
    }
}
