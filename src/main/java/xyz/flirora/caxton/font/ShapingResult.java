package xyz.flirora.caxton.font;

/*
 0 | glyph id, plus (1 << 16) if unsafe_to_break
 1 | cluster index
 2 | x advance
 3 | y advance
 4 | x offset
 5 | y offset
 */
public record ShapingResult(int[] data, int totalWidth) {
    public ShapingResult {
        if (data.length % 6 != 0) {
            throw new IllegalArgumentException("length of data must be divisible by 6");
        }
    }

    public int numGlyphs() {
        return data.length / 6;
    }

    public int glyphId(int i) {
        return data[6 * i] & 0xFFFF;
    }

    public boolean isUnsafeToBreak(int i) {
        return (data[6 * i] & 0x10000) != 0;
    }

    public int clusterIndex(int i) {
        return data[6 * i + 1];
    }

    public int advanceX(int i) {
        return data[6 * i + 2];
    }

    public int advanceY(int i) {
        return data[6 * i + 3];
    }

    public int offsetX(int i) {
        return data[6 * i + 4];
    }

    public int offsetY(int i) {
        return data[6 * i + 5];
    }

    public String toString() {
        StringBuilder builder = new StringBuilder("ShapingResult[glyphs=[");
        for (int i = 0; i < numGlyphs(); ++i) {
            if (i > 0) {
                builder.append("|");
            }
            builder.append(glyphId(i));
            if (isUnsafeToBreak(i)) {
                builder.append("!");
            }
            builder.append("=");
            builder.append(clusterIndex(i));
            int offsetX = offsetX(i), offsetY = offsetY(i), advanceX = advanceX(i), advanceY = advanceY(i);
            if (offsetX != 0 || offsetY != 0) {
                builder.append("@");
                builder.append(offsetX);
                builder.append(",");
                builder.append(offsetY);
            }
            builder.append("+");
            builder.append(advanceX);
            if (advanceY != 0) {
                builder.append(",");
                builder.append(advanceY);
            }
        }
        builder.append("], totalWidth=");
        builder.append(totalWidth);
        builder.append("]");
        return builder.toString();
    }
}
