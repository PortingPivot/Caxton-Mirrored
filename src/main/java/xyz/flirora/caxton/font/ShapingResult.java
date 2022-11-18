package xyz.flirora.caxton.font;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/*
 0 | glyph id, plus (1 << 16) if unsafe_to_break
 1 | cluster index
 2 | x advance
 3 | y advance
 4 | x offset
 5 | y offset

 note to self: glyphs are returned in visual order!
 This means that the codepoint indices will be returned in descending order for RTL text.
 */
@Environment(EnvType.CLIENT)
public record ShapingResult(int[] data, int totalWidth, int totalLength) {
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

    public int clusterLimit(int i) {
        int curr = clusterIndex(i);
        int prev = i == 0 ? -1 : this.clusterIndex(i - 1);
        int next = i == this.numGlyphs() - 1 ? -1 : this.clusterIndex(i + 1);
        if (prev < 0 && next < curr) {
            prev = this.totalLength;
        }
        if (next < 0 && prev < curr) {
            next = this.totalLength;
        }
        return prev > next ? prev : next;
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
