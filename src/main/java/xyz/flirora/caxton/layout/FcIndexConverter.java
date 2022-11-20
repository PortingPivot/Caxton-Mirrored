package xyz.flirora.caxton.layout;

/**
 * Converts between <i>formatless</i> and <i>formatful</i> indices.
 */
public class FcIndexConverter extends ForwardTraversedMap {
    public int formatlessToFormatful(int index) {
        return index + 2 * this.inf(index);
    }

    public int formatlessToFormatful(int index, boolean save) {
        return index + 2 * this.infSlow(index, save);
    }

    public int formatfulToFormatless(int index) {
        int val = this.infp(index, 2);
        if (getLastResultIndex() < size() - 1) {
            int nextKey = getLastResultKey(1);
            int nextValue = getLastResultValue(1);
            int increment = nextValue - val;
            int offset = (nextKey + 2 * nextValue) - index;
            if (offset < 2 * increment) {
                return nextKey;
            }
        }
        return index - 2 * val;
    }

    public int formatfulToFormatless(int index, boolean save) {
        int k = this.arginfpSlow(index, 2);
        int val = this.entries.getInt(2 * k);
        if (k < size() - 1) {
            int nextKey = this.entries.getInt(2 * k + 2);
            int nextValue = this.entries.getInt(2 * k + 3);
            int increment = nextValue - val;
            int offset = (nextKey + 2 * nextValue) - index;
            if (offset < 2 * increment) {
                return nextKey;
            }
        }
        if (save) this.lastAccessedIndex = k;
        return index - 2 * val;
    }
}
