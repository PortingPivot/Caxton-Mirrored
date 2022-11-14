package xyz.flirora.caxton.font;

import java.nio.ByteBuffer;

public class CaxtonInternal {
    // This takes options as a string in order to simplify handling on the Rust side.
    public static native long createFont(ByteBuffer fontData, String cachePath, String options);

    public static native void destroyFont(long addr);

    public static native int fontGlyphIndex(long addr, int codePoint);

    public static native short[] fontMetrics(long addr);

    public static native int fontAtlasSize(long addr);

    public static native long fontAtlasLocations(long addr);

    public static native long fontBboxes(long addr);

    public static native int fontAtlasNumPages(long addr);

    public static native long fontAtlasPage(long addr, int pageNum);

    public static native long configureFont(long fontAddr, String settings);

    public static native void destroyConfiguredFont(long addr);

    public static native ShapingResult[] shape(long fontAddr, char[] s, int[] bidiRuns);
}
