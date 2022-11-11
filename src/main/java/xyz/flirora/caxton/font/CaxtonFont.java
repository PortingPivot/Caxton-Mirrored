package xyz.flirora.caxton.font;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;

/**
 * Holds information related to a single font file.
 */
@Environment(EnvType.CLIENT)
public class CaxtonFont implements AutoCloseable {
    private static String cacheDir = null;
    private final Identifier id;
    private final int unitsPerEm;
    private ByteBuffer fontData;
    private long fontPtr;

    public CaxtonFont(InputStream input, Identifier id) throws IOException {
        this.id = id;

        try {
            byte[] readInput = input.readAllBytes();

            fontData = MemoryUtil.memAlloc(readInput.length);
            fontData.put(readInput);
            fontPtr = CaxtonInternal.createFont(
                    fontData,
                    getCacheDir());
            unitsPerEm = CaxtonInternal.fontUnitsPerEm(fontPtr);
        } catch (Exception e) {
            try {
                this.close();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        CaxtonInternal.destroyFont(fontPtr);
        fontPtr = 0;
        MemoryUtil.memFree(fontData);
        fontData = null;
    }

    public boolean supportsCodePoint(int codePoint) {
        return CaxtonInternal.fontGlyphIndex(fontPtr, codePoint) != -1;
    }

    public int getUnitsPerEm() {
        return unitsPerEm;
    }

    public ShapingResult[] shape(char[] s, int[] bidiRuns) {
        return CaxtonInternal.shape(fontPtr, s, bidiRuns);
    }

    private String getCacheDir() {
        if (cacheDir == null) {
            var dir = new File(MinecraftClient.getInstance().runDirectory, "caxton_cache");
            try {
                Files.createDirectories(dir.toPath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            cacheDir = dir.getAbsolutePath();
        }
        return cacheDir;
    }
}
