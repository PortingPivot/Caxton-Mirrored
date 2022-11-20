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

    public int valueOfMaxKey() {
        return entries.getInt(entries.size() - 1);
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
