package xyz.flirora.caxton.font;

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
    private static final boolean DEBUG = true;
    private final List<Run> styleRuns;
    private final char[] joined;
    // The fields below are null when getFont() is null.
    private final int @Nullable [] bidiRuns;
    private final int @Nullable [] styleRunStarts;
    // Cached results for getting the style run associated with a string index,
    // optimized for sequential access.
    private int lastQueriedStylePosition = 0, lastQueriedStyleResult = 0;

    public RunGroup(List<Run> styleRuns) {
        // BreakIterator breakIterator = BreakIterator.getLineInstance();
        if (styleRuns.isEmpty()) {
            throw new IllegalArgumentException("runs may not be empty");
        }
        this.styleRuns = styleRuns;
        String joined = styleRuns.stream().map(Run::text).collect(Collectors.joining());

        if (!DEBUG && styleRuns.get(0).font() == null) {
            // Legacy font; don’t compute bidi info.
            this.bidiRuns = null;
            this.styleRunStarts = null;
        } else {
            // Caxton font; do compute bidi info.
            Bidi bidi = new Bidi(joined, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
            int numberOfRuns = bidi.countRuns();
            this.bidiRuns = new int[2 * numberOfRuns];
            for (int i = 0; i < numberOfRuns; ++i) {
                this.bidiRuns[2 * i] = bidi.getRunStart(i);
                this.bidiRuns[2 * i + 1] = bidi.getRunLimit(i);
            }

            this.styleRunStarts = new int[styleRuns.size() + 1];
            int x = 0;
            for (int i = 0; i < styleRuns.size(); ++i) {
                this.styleRunStarts[i] = x;
                x += styleRuns.get(i).text().length();
            }
            this.styleRunStarts[styleRuns.size()] = x;
        }

        this.joined = joined.toCharArray();
    }

    public @Nullable CaxtonFont getFont() {
        return styleRuns.get(0).font();
    }

    public String toString() {
        return "RunGroup[runs=" + styleRuns + ", bidiRuns=" + Arrays.toString(bidiRuns) + ", styleRunStarts=" + Arrays.toString(styleRunStarts) + ", #=" + joined.length + "]";
    }

    public List<Run> getStyleRuns() {
        return styleRuns;
    }

    public int @Nullable [] getBidiRuns() {
        return bidiRuns;
    }

    public char[] getJoined() {
        return joined;
    }

    public Style getStyleAt(int index) {
        if (index < 0 || index >= this.joined.length) {
            throw new IndexOutOfBoundsException("index must be in [0, " + this.joined.length + "); got " + index);
        }
        int result = getStyleIndexAt(index);
        lastQueriedStylePosition = index;
        lastQueriedStyleResult = result;
        return styleRuns.get(result).style();
    }

    private int getStyleIndexAt(int index) {
        Objects.requireNonNull(styleRunStarts, "this method is not supported for legacy-font runs");
        if (index == lastQueriedStylePosition) return lastQueriedStyleResult;
        if (index == lastQueriedStylePosition + 1) {
            if (index >= styleRunStarts[lastQueriedStyleResult + 1]) {
                return lastQueriedStyleResult + 1;
            }
            return lastQueriedStyleResult;
        }
        if (index == lastQueriedStylePosition - 1) {
            if (index == styleRunStarts[lastQueriedStyleResult]) {
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
    public ShapingResult[] shape(Map<CaxtonFont, Map<String, ShapingResult>> shapingCache) {
        CaxtonFont font = this.getFont();

        if (font == null) {
            throw new UnsupportedOperationException("shapeRunGroup requires a Caxton font (got a legacy font)");
        }

        var shapingCacheForFont = shapingCache.computeIfAbsent(font, f -> new HashMap<>());

        // Determine which runs need to be shaped
        int[] bidiRuns = this.getBidiRuns();
        IntList uncachedBidiRuns = new IntArrayList(bidiRuns.length / 2);
        ShapingResult[] shapingResults = new ShapingResult[bidiRuns.length / 2];
        for (int i = 0; i < bidiRuns.length / 2; ++i) {
            int start = bidiRuns[2 * i];
            int end = bidiRuns[2 * i + 1];
            String s = new String(this.getJoined(), start, end - start);
            ShapingResult sr = shapingCacheForFont.get(s);
            if (sr != null) {
                shapingResults[i] = sr;
            } else {
                uncachedBidiRuns.add(start);
                uncachedBidiRuns.add(end);
            }
        }

        if (!uncachedBidiRuns.isEmpty()) {
            ShapingResult[] newlyComputed = font.shape(this.getJoined(), uncachedBidiRuns.toIntArray());

            // Fill in blanks from before
            for (int i = 0, j = 0; i < bidiRuns.length / 2; ++i) {
                if (shapingResults[i] == null) {
                    shapingResults[i] = newlyComputed[j];

                    int start = bidiRuns[2 * i];
                    int end = bidiRuns[2 * i + 1];
                    String s = new String(this.getJoined(), start, end - start);
                    shapingCacheForFont.put(s, newlyComputed[j]);

                    ++j;
                }
            }
        }

        return shapingResults;
    }
}
