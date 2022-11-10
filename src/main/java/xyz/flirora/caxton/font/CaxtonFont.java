package xyz.flirora.caxton.font;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;

/**
 * Holds information related to a single font file.
 */
public class CaxtonFont implements AutoCloseable {
    private static String cacheDir = null;
    private final ByteBuffer fontData;
    private final long fontPtr;

    public CaxtonFont(InputStream input) throws IOException {
        try {
            byte[] readInput = input.readAllBytes();

            fontData = MemoryUtil.memAlloc(readInput.length);
            fontData.put(readInput);
            fontPtr = CaxtonInternal.createFont(
                    fontData,
                    getCacheDir());
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
    public void close() throws Exception {
        MemoryUtil.memFree(fontData);
        CaxtonInternal.destroyFont(fontPtr);
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
