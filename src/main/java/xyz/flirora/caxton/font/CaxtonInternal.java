package xyz.flirora.caxton.font;

import java.nio.ByteBuffer;

public class CaxtonInternal {
    public static native long createFont(ByteBuffer fontData, String cachePath);

    public static native void destroyFont(long addr);

    public static native int fontGlyphIndex(long addr, int codePoint);

    public static native short[] fontMetrics(long addr);

    public static native ShapingResult[] shape(long fontAddr, char[] s, int[] bidiRuns);
}
