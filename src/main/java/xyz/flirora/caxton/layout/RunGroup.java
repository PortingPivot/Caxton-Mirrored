package xyz.flirora.caxton.layout;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.ArabicShaping;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.CharacterVisitor;
import net.minecraft.text.Style;
import org.jetbrains.annotations.Nullable;
import xyz.flirora.caxton.font.ConfiguredCaxtonFont;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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

    // The fields below are null when getFont() is null.
    // The codepoint offset at which each style run starts
    private final int @Nullable [] styleRunStarts;
    // The shaping results for this run group.
    // Null if explicitly told not to compute these.
    private final ShapingResult @Nullable [] shapingResults;
    // Cached results for getting the style run associated with a string index,
    // optimized for sequential access.
    private int lastQueriedStylePosition = 0, lastQueriedStyleResult = 0;

    /**
     * Constructs a new {@link RunGroup}.
     *
     * @param styleRuns  a list of style runs
     * @param runLevel   the overall bidi level of this run
     * @param charOffset the offset of this run group relative to the entire {@link CaxtonText}, in UTF-16 code units
     * @param bidiRuns   an array of integers consisting of interleaved {@code [start, end, level]} triples
     * @param cache      a {@link LayoutCache} to use when computing shaping results. If this is null, then no shaping results will be computed.
     */
    public RunGroup(List<Run> styleRuns, int runLevel, int charOffset, int[] bidiRuns, @Nullable LayoutCache cache) {
        this.runLevel = runLevel;
        this.charOffset = charOffset;
        // BreakIterator breakIterator = BreakIterator.getLineInstance();
        if (styleRuns.isEmpty()) {
            throw new IllegalArgumentException("runs may not be empty");
        }
        this.styleRuns = styleRuns;
        String joined = styleRuns.stream().map(Run::text).collect(Collectors.joining());
        this.joined = joined.toCharArray();
//        if (runLevel % 2 != 0) System.err.println(styleRuns + "@" + charOffset + ": " + Arrays.toString(bidiRuns));

        this.bidiRuns = bidiRuns;

        this.styleRunStarts = new int[styleRuns.size() + 1];
        int x = 0;
        for (int i = 0; i < styleRuns.size(); ++i) {
            this.styleRunStarts[i] = x;
            x += styleRuns.get(i).text().length();
        }
        this.styleRunStarts[styleRuns.size()] = x;

        if (styleRuns.get(0).font() == null) {
            this.shapingResults = null;
        } else {
            if (cache == null) {
                this.shapingResults = null;
            } else {
                this.shapingResults = shape(cache);
            }
        }
    }

    /**
     * Visits the text in visual order.
     * <p>
     * This should only be used for handling text in legacy fonts; for text in Caxton fonts, it is better to use {@link RunGroup#getShapingResults}.
     * <p>
     * Currently, this does not implement legacy Arabic shaping. This issue arises from the limitations of the {@link ArabicShaping} API.
     *
     * @param visitor The {@link DirectionalCharacterVisitor} to use for visiting the text.
     * @return true if the traversal was completed without interruption; false if it was interrupted.
     */
    public boolean accept(DirectionalCharacterVisitor visitor) {
        ArabicShaping shaper = new ArabicShaping(ArabicShaping.LETTERS_SHAPE | ArabicShaping.TEXT_DIRECTION_VISUAL_LTR);
        for (int i = 0; i < bidiRuns.length / 3; ++i) {
            int start = bidiRuns[3 * i];
            int end = bidiRuns[3 * i + 1];
            int level = bidiRuns[3 * i + 2];
            if (level % 2 != 0) {
                // RTL
                int j = end;
                while (j > start) {
                    char c = joined[j - 1];
                    int cp = c;
                    if (Character.isLowSurrogate(c)) {
                        if (j >= start + 1 && Character.isHighSurrogate(joined[j - 2])) {
                            cp = Character.toCodePoint(joined[j - 1], c);
                            --j;
                        } else {
                            cp = '\uFFFD';
                        }
                    } else if (Character.isHighSurrogate(c)) {
                        cp = '\uFFFD';
                    }
                    --j;
                    if (!visitor.accept(j, getStyleAt(j), UCharacter.getMirror(cp), true))
                        return false;
                }
            } else {
                // LTR
                int j = start;
                while (j < end) {
                    int k = j;
                    char c = joined[j];
                    int cp = c;
                    if (Character.isHighSurrogate(c)) {
                        if (j < end - 1 && Character.isLowSurrogate(joined[j + 1])) {
                            cp = Character.toCodePoint(c, joined[j + 1]);
                            ++j;
                        } else {
                            cp = '\uFFFD';
                        }
                    } else if (Character.isLowSurrogate(c)) {
                        cp = '\uFFFD';
                    }
                    ++j;
                    if (!visitor.accept(k, getStyleAt(k), cp, false))
                        return false;
                }
            }
        }
        return true;
    }

    /**
     * Visits the text in visual order.
     * <p>
     * This should only be used for handling text in legacy fonts; for text in Caxton fonts, it is better to use {@link RunGroup#getShapingResults}.
     * <p>
     * Currently, this does not implement legacy Arabic shaping. This issue arises from the limitations of the {@link ArabicShaping} API.
     *
     * @param visitor The {@link CharacterVisitor} to use for visiting the text.
     * @return true if the traversal was completed without interruption; false if it was interrupted.
     */
    public boolean accept(CharacterVisitor visitor) {
        return accept(DirectionalCharacterVisitor.fromCharacterVisitor(visitor));
    }

    /**
     * Gets the {@link ConfiguredCaxtonFont} that was used for this run group.
     *
     * @return the {@link ConfiguredCaxtonFont} used, or {@code null} if this run group uses a legacy font
     */
    public @Nullable ConfiguredCaxtonFont getFont() {
        return styleRuns.get(0).font();
    }

    public String toString() {
        return "RunGroup[runs=" + styleRuns + ", bidiRuns=" + Arrays.toString(bidiRuns) + ", styleRunStarts=" + Arrays.toString(styleRunStarts) + ", charOffset=" + charOffset + ", runLevel=" + runLevel + ", #=" + joined.length + "]";
    }

    /**
     * Returns the list of style runs in logical order.
     *
     * @return a list of {@link Run}s
     * @deprecated This is probably not what you want. For rendering legacy-font text, use {@link RunGroup#accept} instead.
     */
    @Deprecated(forRemoval = false)
    public List<Run> getStyleRuns() {
        return styleRuns;
    }

    /**
     * Gets the bidi runs of this run group.
     * <p>
     * Note that the start and end indices are relative to the start of this run group, not the start of the {@link CaxtonText} in which it lies.
     * <p>
     * The returned array must not be modified.
     *
     * @return an array of integers consisting of interleaved {@code [start, end, level]} triples
     */
    public int[] getBidiRuns() {
        return bidiRuns;
    }

    /**
     * Gets the text of this run group, without formatting.
     * <p>
     * The returned array must not be modified. This function returns a {@code char[]} instead of a {@link String} in order to avoid the overhead of JNI’s string conversion.
     *
     * @return an array of characters representing the text of this run group
     */
    public char[] getJoined() {
        return joined;
    }

    /**
     * Gets the overall run level of this run group.
     * <p>
     * Currently, this is the run level of the first bidi run of this group.
     *
     * @return the bidi run level of this group: even if it is left-to-right and odd if it is right-to-left
     */
    public int getRunLevel() {
        return runLevel;
    }

    /**
     * Gets the offset of this run group from the start of the {@link CaxtonText} that it is in.
     *
     * @return an offset in UTF-16 code units
     */
    public int getCharOffset() {
        return charOffset;
    }

    /**
     * Gets the length of the text in this run group
     *
     * @return total number of UTF-16 code units for this run group
     */
    public int getTotalLength() {
        return joined.length;
    }

    public boolean containsIndex(int index) {
        return index >= charOffset && index < charOffset + joined.length;
    }

    /**
     * Gets the style used at an index.
     * <p>
     * This caches the results of the last call in order to optimize sequential access.
     *
     * @param index the index of the character relative to the start of this run group
     * @return the {@link Style} used at the {@code index}th char
     */
    public Style getStyleAt(int index) {
        int result = getStyleIndexAt(index);
        return styleRuns.get(result).style();
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
     * Gets the computed shaping results.
     * <p>
     * The returned array must not be modified.
     *
     * @return an arary of {@link ShapingResult}s, if present
     * @see RunGroup#RunGroup(List, int, int, int[], LayoutCache)
     * @see ConfiguredCaxtonFont#shape(char[], int[])
     */
    public ShapingResult[] getShapingResults() {
        return shapingResults;
    }

    /**
     * Shape each bidi run of this run group, using a cache.
     *
     * @param cache a {@link LayoutCache} for getting and setting cached results
     * @return an array of {@link ShapingResult}s for each bidi run
     */
    private ShapingResult[] shape(LayoutCache cache) {
        ConfiguredCaxtonFont font = this.getFont();

        if (font == null) {
            throw new UnsupportedOperationException("shapeRunGroup requires a Caxton font (got a legacy font)");
        }

        var shapingCacheForFont = cache.getShapingCacheFor(font);

        // Determine which runs need to be shaped
        int[] bidiRuns = this.getBidiRuns();
        IntList uncachedBidiRuns = new IntArrayList(bidiRuns.length);
        ShapingResult[] shapingResults = new ShapingResult[bidiRuns.length / 3];
//        var cachedShapingResults = shapingCacheForFont.getAllPresent();
        for (int i = 0; i < bidiRuns.length / 3; ++i) {
            int start = bidiRuns[3 * i];
            int end = bidiRuns[3 * i + 1];
            int level = bidiRuns[3 * i + 2];
            String s = new String(this.getJoined(), start, end - start);
            ShapedString key = new ShapedString(s, level % 2 != 0);
            ShapingResult sr = shapingCacheForFont.getIfPresent(key);
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
