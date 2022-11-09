package xyz.flirora.caxton.msdf;

import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.client.texture.NativeImage;

import java.util.ArrayList;

/**
 * A glyph atlas.
 * <p>
 * Instead of using a class to describe locations in the atlas, this class
 * uses a <code>long</code> with the following layout:
 *
 * <ul>
 *  <li>bits 0 – 11: x</li>
 *  <li>bits 12 – 23: y</li>
 *  <li>bits 24 – 35: width</li>
 *  <li>bits 36 – 47: height</li>
 *  <li>bits 48 – 63: page number</li>
 * </ul>
 */
public class GlyphAtlas implements AutoCloseable {
    private static final int MAX_SIZE = 4096;
    private final ArrayList<NativeImage> pages;
    private final Int2LongMap glyphLocations;

    private GlyphAtlas() {
        this.pages = new ArrayList<>();
        this.glyphLocations = new Int2LongOpenHashMap();
    }

    public static long encode(int x, int y, int width, int height, int page) {
        if (x < 0 || x >= MAX_SIZE) throw new IllegalArgumentException("x must be in [0, 4096)");
        if (y < 0 || y >= MAX_SIZE) throw new IllegalArgumentException("y must be in [0, 4096)");
        if (width < 0 || width >= MAX_SIZE) throw new IllegalArgumentException("width must be in [0, 4096)");
        if (height < 0 || height >= MAX_SIZE) throw new IllegalArgumentException("height must be in [0, 4096)");
        if (page < 0 || page >= 65536) throw new IllegalArgumentException("page must be in [0, 4096)");
        return ((long) x)
                | (((long) y) << 12)
                | (((long) width) << 24)
                | (((long) height) << 32)
                | (((long) page) << 48);
    }

    public static int getX(long q) {
        return (int) (q & 0xFFF);
    }

    public static int getY(long q) {
        return (int) ((q >> 12) & 0xFFF);
    }

    public static int getWidth(long q) {
        return (int) ((q >> 24) & 0xFFF);
    }

    public static int getHeight(long q) {
        return (int) ((q >> 36) & 0xFFF);
    }

    public static int getPageIndex(long q) {
        return (int) (q >>> 48);
    }

    // the glyph atlas can be recovered with build()
    @SuppressWarnings("resource")
    public static Builder builder() {
        return new GlyphAtlas().new Builder();
    }

    @Override
    public void close() throws Exception {
        for (NativeImage page : pages) {
            page.close();
        }
    }

    public long getGlyph(int codepoint) {
        return glyphLocations.get(codepoint);
    }

    public NativeImage getPage(int index) {
        return pages.get(index);
    }

    public class Builder {
        private final LongList emptySpaces;

        private Builder() {
            this.emptySpaces = new LongArrayList();
        }

        public long insert(int codepoint, int width, int height) {
            if (width > MAX_SIZE || height > MAX_SIZE) {
                throw new IllegalArgumentException("Image to be inserted is too big");
            }
            if (glyphLocations.containsKey(codepoint)) {
                throw new UnsupportedOperationException("cannot insert the same codepoint twice");
            }
            long space = tryInsert(width, height);
            if (space == -1) {
                addNewPage();
                space = tryInsert(width, height);
            }
            glyphLocations.put(codepoint, space);
            return space;
        }

        private long tryInsert(int width, int height) {
            for (int i = emptySpaces.size() - 1; i >= 0; --i) {
                long space = emptySpaces.getLong(i);
                int spaceW = getWidth(space);
                int spaceH = getHeight(space);
                int spaceX = getX(space);
                int spaceY = getY(space);
                int spaceP = getPageIndex(space);
                if (spaceW < width || spaceH < height) continue;
                emptySpaces.set(i, emptySpaces.getLong(emptySpaces.size()) - 1);
                emptySpaces.removeLong(emptySpaces.size() - 1);
                if (spaceH > height) {
                    long below = encode(spaceX, spaceY + height, spaceW, spaceH - height, spaceP);
                    emptySpaces.add(below);
                }
                if (spaceW > width) {
                    long right = encode(spaceX + width, spaceY, spaceW - width, spaceH, spaceP);
                    emptySpaces.add(right);
                }
                return encode(spaceX, spaceY, width, height, spaceP);
            }
            return -1;
        }

        private void addNewPage() {
            int index = pages.size();
            pages.add(new NativeImage(MAX_SIZE, MAX_SIZE, false));
            emptySpaces.add(encode(0, 0, MAX_SIZE, MAX_SIZE, index));
        }

        public GlyphAtlas build() {
            return GlyphAtlas.this;
        }
    }
}
