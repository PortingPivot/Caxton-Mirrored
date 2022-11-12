package xyz.flirora.caxton.font;

import com.google.gson.JsonObject;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
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
    private final short[] metrics;
    private final int atlasSize;
    private final long atlasLocations;
    private final long bboxes;
    // Intentionally not closed because this does not own the texture memory.
    private CaxtonAtlasTexture[] pages;
    private CaxtonFontOptions options;
    private ByteBuffer fontData;
    private long fontPtr;
    private boolean registered = false;

    public CaxtonFont(InputStream input, Identifier id, JsonObject options) throws IOException {
        this.id = id;

        try {
            byte[] readInput = input.readAllBytes();

            fontData = MemoryUtil.memAlloc(readInput.length);
            fontData.put(readInput);
            fontPtr = CaxtonInternal.createFont(
                    fontData,
                    getCacheDir(),
                    options.toString());
            metrics = CaxtonInternal.fontMetrics(fontPtr);
            atlasSize = CaxtonInternal.fontAtlasSize(fontPtr);
            atlasLocations = CaxtonInternal.fontAtlasLocations(fontPtr);
            bboxes = CaxtonInternal.fontBboxes(fontPtr);
            int numPages = CaxtonInternal.fontAtlasNumPages(fontPtr);
            pages = new CaxtonAtlasTexture[numPages];
            this.options = new CaxtonFontOptions(options);
            for (int i = 0; i < numPages; ++i) {
                pages[i] = new CaxtonAtlasTexture(this, fontPtr, i);
            }
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
        for (NativeImageBackedTexture page : pages) {
            page.close();
        }
        pages = null;
        CaxtonInternal.destroyFont(fontPtr);
        fontPtr = 0;
        MemoryUtil.memFree(fontData);
        fontData = null;
    }

    public String toString() {
        return "CaxtonFont[" + id + "@" + Long.toHexString(fontPtr) + "]";
    }

    public boolean supportsCodePoint(int codePoint) {
        return CaxtonInternal.fontGlyphIndex(fontPtr, codePoint) != -1;
    }

    public Identifier getId() {
        return id;
    }

    public short getMetrics(int i) {
        return metrics[i];
    }

    public CaxtonFontOptions getOptions() {
        return options;
    }

    public ShapingResult[] shape(char[] s, int[] bidiRuns) {
        return CaxtonInternal.shape(fontPtr, s, bidiRuns);
    }

    public long getAtlasLocation(int glyphId) {
        if (glyphId < 0 || glyphId >= atlasSize) {
            throw new IndexOutOfBoundsException("i must be in [0, " + atlasSize + ") (got " + glyphId + ")");
        }
        return MemoryUtil.memGetLong(atlasLocations + 8 * ((long) glyphId));
    }

    public long getBbox(int glyphId) {
        if (glyphId < 0 || glyphId >= atlasSize) {
            throw new IndexOutOfBoundsException("i must be in [0, " + atlasSize + ") (got " + glyphId + ")");
        }
        return MemoryUtil.memGetLong(bboxes + 8 * ((long) glyphId));
    }

    public CaxtonAtlasTexture getAtlasPage(int i) {
        return this.pages[i];
    }

    public void registerTextures(TextureManager textureManager) {
        if (registered) return;
        for (CaxtonAtlasTexture page : pages) {
            textureManager.registerTexture(page.getId(), page);
        }
        registered = true;
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

    public static class Metrics {
        public static int UNITS_PER_EM = 0;
        public static int ASCENDER = 1;
        public static int DESCENDER = 2;
        public static int HEIGHT = 3;
        public static int LINE_GAP = 4;
        public static int UNDERLINE_POSITION = 5;
        public static int UNDERLINE_THICKNESS = 6;
    }
}
