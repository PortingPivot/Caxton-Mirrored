package xyz.flirora.caxton.font;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Holds information related to a single font file.
 */
public class CaxtonFont implements AutoCloseable {
    private final ByteBuffer fontData;
    private final long fontPtr;

    public CaxtonFont(InputStream input) throws IOException {
        try {
            byte[] readInput = input.readAllBytes();

            fontData = MemoryUtil.memAlloc(readInput.length);
            fontData.put(readInput);
            fontPtr = CaxtonInternal.createFont(
                    fontData,
                    MinecraftClient.getInstance().runDirectory.getAbsolutePath() + "/caxton_cache");
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
}
