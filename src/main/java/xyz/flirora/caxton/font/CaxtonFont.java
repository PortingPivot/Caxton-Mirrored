package xyz.flirora.caxton.font;

import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Holds information related to a single font file.
 */
@Environment(EnvType.CLIENT)
public class CaxtonFont implements AutoCloseable {
    private static final boolean DEBUG_REFCOUNT_CHANGES = false;
    private static String cacheDir = null;
    private final Identifier id;
    private final short[] metrics;
    private final int atlasSize;
    private final long atlasLocations;
    private final long bboxes;
    private final CaxtonFontOptions options;
    private final Int2ObjectMap<IntList> glyphsByWidth;
    private final List<Pair<StackTraceElement[], Boolean>> changes;
    private CaxtonAtlasTexture[] pages;
    private ByteBuffer fontData;
    private long fontPtr;
    private boolean registered = false;
    // TODO: does this need to be atomic?
    private int refCount = 0;

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
            this.glyphsByWidth = new Int2ObjectOpenHashMap<>();
            for (int glyphId = 0; glyphId < atlasSize; ++glyphId) {
                long atlasLoc = MemoryUtil.memGetLong(atlasLocations + 8 * ((long) glyphId));
                int width = (int) ((atlasLoc >> 26) & 0x1FFF);
                this.glyphsByWidth.computeIfAbsent(width, w -> new IntArrayList())
                        .add(glyphId);
            }
            this.changes = DEBUG_REFCOUNT_CHANGES ? new ArrayList<>() : null;
        } catch (Exception e) {
            try {
                this.cloneReference();
                this.close();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        int remainingRefs = --this.refCount;
        if (remainingRefs < 0) {
            System.err.println("ERROR: Font closed with a refcount of 0.");
            if (this.changes != null) {
                System.err.println("Refcount changes:");
                for (var change : this.changes) {
                    System.err.println(change.second() ? "Reference added in:" : "Reference removed in:");
                    for (var elem : change.first()) {
                        System.err.println(elem);
                    }
                }
            }
            throw new IllegalStateException("font closed with a refcount of 0");
        }
        if (remainingRefs == 0) {
            if (pages != null) {
                for (NativeImageBackedTexture page : pages) {
                    if (page != null) page.close();
                }
            }
            pages = null;
            CaxtonInternal.destroyFont(fontPtr);
            fontPtr = 0;
            MemoryUtil.memFree(fontData);
            fontData = null;
        }
        if (this.changes != null) {
            this.changes.add(Pair.of(Thread.currentThread().getStackTrace(), false));
        }
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

    public long getFontPtr() {
        return fontPtr;
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

    public Int2ObjectMap<IntList> getGlyphsByWidth() {
        return glyphsByWidth;
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

    public CaxtonFont cloneReference() {
        if (this.changes != null) {
            this.changes.add(Pair.of(Thread.currentThread().getStackTrace(), true));
        }
        ++this.refCount;
        return this;
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
