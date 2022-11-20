package xyz.flirora.caxton.layout;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

/**
 * A map from integers to integers that can only be traversed forward.
 * <p>
 * This means that any retrieval of a value must be with a key that is greater than or equal to the last key used for retrieval. If this condition can be held, then <var>k</var> retrievals to a map with <var>n</var> entries can be done in a total of <var>O</var>(<var>n</var> + <var>k</var>) time.
 */
public class ForwardTraversedMap {
    private final IntList entries = new IntArrayList();
    private int lastAccessedIndex = 0;

    /**
     * Constructs an empty {@link ForwardTraversedMap}.
     */
    public ForwardTraversedMap() {
    }

    /**
     * Resets the state of lookups for this map, so that it acts if {@link ForwardTraversedMap#inf(int)} and {@link ForwardTraversedMap#infp(int)} had never been called.
     */
    public void reset() {
        lastAccessedIndex = 0;
    }

    /**
     * Gets the value associated with the greatest key in this map that is less than {@code key}.
     *
     * @param key The key; must be greater than or equal to the value of {@code key} passed into the previous invocation of this method, and must be greater than or equal to the first key added to this map.
     * @return The value associated with the greatest key in this map that is strictly less than {@code key}.
     */
    public int inf(int key) {
        if (key < entries.getInt(2 * lastAccessedIndex)) {
            throw new IllegalArgumentException("detected inf with descending key");
        }
        while (lastAccessedIndex < size() - 1 && entries.getInt(2 * lastAccessedIndex + 2) < key) {
            ++lastAccessedIndex;
        }
        return entries.getInt(2 * lastAccessedIndex + 1);
    }

    /**
     * This method requires the entries in the map to be monotonic in terms of the sum of the key and value as well. It acts similarly to {@link ForwardTraversedMap#inf(int)} but treats each key in the map as if it had {@code factor} times the corresponding value added to it.
     *
     * @param key The key; must be greater than or equal to the value of {@code key} passed into the previous invocation of this method, and must be greater than or equal to the first key added to this map.
     * @return The value associated with the last entry in this map such that {@code k + factor * v} is strictly less than {@code key}.
     */
    public int infp(int key, int factor) {
        if (key < entries.getInt(2 * lastAccessedIndex)) {
            throw new IllegalArgumentException("detected inf with descending key");
        }
        while (lastAccessedIndex < size() - 1 && entries.getInt(2 * lastAccessedIndex + 2) + factor * entries.getInt(2 * lastAccessedIndex + 3) < key) {
            ++lastAccessedIndex;
        }
        return entries.getInt(2 * lastAccessedIndex + 1);
    }

    public int getLastResultIndex() {
        return lastAccessedIndex;
    }

    public int getLastResultKey() {
        return entries.getInt(2 * lastAccessedIndex);
    }

    public int getLastResultKey(int offset) {
        return entries.getInt(2 * (lastAccessedIndex + offset));
    }

    public int getLastResultValue(int offset) {
        return entries.getInt(2 * (lastAccessedIndex + offset) + 1);
    }

    public int valueOfMaxKey() {
        return entries.getInt(entries.size() - 1);
    }

    public int infSlow(int key, boolean save) {
        int lower = 0, upper = size();
        while (upper > lower + 1) {
            int mid = (lower + upper) >>> 1;
            int midVal = entries.getInt(2 * mid);
            if (midVal >= key) {
                upper = mid;
            } else {
                lower = mid;
            }
        }
        if (save) lastAccessedIndex = lower;
        return entries.getInt(2 * lower + 1);
    }

    public int infpSlow(int key, int factor, boolean save) {
        int lower = 0, upper = size();
        while (upper > lower + 1) {
            int mid = (lower + upper) >>> 1;
            int midVal = entries.getInt(2 * mid) + factor * entries.getInt(2 * mid + 1);
            if (midVal >= key) {
                upper = mid;
            } else {
                lower = mid;
            }
        }
        if (save) lastAccessedIndex = lower;
        return entries.getInt(2 * lower + 1);
    }

    /**
     * Adds a key–value pair to the map. This method must be called with key values in nondescending order. If the key is the same as the key of the last pair added with this method, then this invocation replaces the old value.
     *
     * @param key   The key for this map. Must be greater than or equal to the value of {@code key} passed into the previous invocation of this method.
     * @param value The value to associate with this key.
     */
    public void put(int key, int value) {
        int size = size();
        if (size > 0 && entries.getInt(2 * size - 2) == key) {
            entries.set(2 * size - 1, value);
        } else {
            entries.add(key);
            entries.add(value);
        }
    }

    public int size() {
        return entries.size() / 2;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder("{");
        for (int i = 0; i < size(); ++i) {
            if (i != 0) builder.append(", ");
            if (i == lastAccessedIndex) builder.append('(');
            builder.append(entries.getInt(2 * i));
            builder.append("=>");
            builder.append(entries.getInt(2 * i + 1));
            if (i == lastAccessedIndex) builder.append(')');
        }
        builder.append('}');
        return builder.toString();
    }
}
