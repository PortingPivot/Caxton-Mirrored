package xyz.flirora.caxton.font;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
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
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds information related to a single font file.
 *
 * @see ConfiguredCaxtonFont
 * @see CaxtonTypeface
 */
@Environment(EnvType.CLIENT)
public class CaxtonFont implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
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

    /**
     * Constructs a new {@link CaxtonFont}.
     * <p>
     * The font has an initial reference count of 0; thus, it is recommended to call {@link CaxtonFont#cloneReference()} on the returned object.
     *
     * @param input   the {@link InputStream} containing the font file’s data in TTF or OTF format
     * @param id      the {@link Identifier} under which this font should be known
     * @param options options to pass during font generation as a JSON object
     * @throws IOException if the underlying I/O operations fail
     */
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

    /**
     * Closes the font.
     * <p>
     * This might not actually free the font data; more precisely, it decreases its reference count. Once the reference count becomes zero, the underlying resources to the font are freed.
     *
     * @throws IllegalStateException if this method is called when the font has no remaining references
     */
    @Override
    public void close() {
        int remainingRefs = --this.refCount;
        if (remainingRefs < 0) {
            LOGGER.error("ERROR: Font closed with a refcount of 0.");
            if (this.changes != null) {
                LOGGER.error("Refcount changes:");
                for (var change : this.changes) {
                    LOGGER.error(change.second() ? "Reference added in:" : "Reference removed in:");
                    for (var elem : change.first()) {
                        LOGGER.error(elem.toString());
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

    /**
     * Returns whether this font has a glyph for a given code point.
     *
     * @param codePoint a Unicode code point
     * @return {@code true} if this font supports this code point
     */
    public boolean supportsCodePoint(int codePoint) {
        return CaxtonInternal.fontGlyphIndex(fontPtr, codePoint) != -1;
    }

    /**
     * Gets the identifier of this font.
     *
     * @return the {@link Identifier} of this font
     */
    public Identifier getId() {
        return id;
    }

    /**
     * Gets the value of metric from this font.
     *
     * @param i one of the values in {@link Metrics}
     * @return the value for the given metric
     * @see Metrics
     */
    public short getMetrics(int i) {
        return metrics[i];
    }

    /**
     * Gets the options associated with this font.
     *
     * @return the {@link CaxtonFontOptions} for this font
     */
    public CaxtonFontOptions getOptions() {
        return options;
    }

    /**
     * Gets the raw pointer to this font.
     *
     * @return the address of the pointer to the font in native memory
     */
    public long getFontPtr() {
        return fontPtr;
    }

    /**
     * Gets the location of glyph number {@code glyphId} in the glyph atlas.
     * <p>
     * This is laid out as such, with bit 0 being the least significant:
     *
     * <ul>
     *     <li><b>Bits 0 – 12:</b> The <var>x</var>-coordinate of the top-left corner of the box, in pixels.</li>
     *     <li><b>Bits 13 – 25:</b> The <var>y</var>-coordinate of the top-left corner of the box, in pixels.</li>
     *     <li><b>Bits 26 – 38:</b> The width of the box, in pixels.</li>
     *     <li><b>Bits 39 – 51:</b> The height of the box, in pixels.</li>
     *     <li><b>Bits 52 – 63:</b> The index of the atlas page in which the glyph lies.</li>
     * </ul>
     * <p>
     * In addition, the glyph atlas leaves a margin of {@code getOptions().margin()} pixels on all sides of the glyph itself. This margin is reflected in the returned value.
     *
     * @param glyphId the ID of the glyph to retrieve the atlas location for
     * @return a packed atlas location value
     */
    public long getAtlasLocation(int glyphId) {
        if (glyphId < 0 || glyphId >= atlasSize) {
            throw new IndexOutOfBoundsException("i must be in [0, " + atlasSize + ") (got " + glyphId + ")");
        }
        return MemoryUtil.memGetLong(atlasLocations + 8 * ((long) glyphId));
    }

    /**
     * Gets the bounding box of glyph number {@param glyphId} as defined in the font file.
     * <p>
     * This is laid out as such, with bit 0 being the least significant:
     *
     * <ul>
     *     <li><b>Bits 0 – 15:</b> The minimum <var>x</var>-coordinate of the bounding box.</li>
     *     <li><b>Bits 16 – 31:</b> The minimum <var>y</var>-coordinate of the bounding box.</li>
     *     <li><b>Bits 32 – 47:</b> The maximum <var>x</var>-coordinate of the bounding box.</li>
     *     <li><b>Bits 48 – 63:</b> The maximum <var>y</var>-coordinate of the bounding box.</li>
     * </ul>
     * <p>
     * Unlike the coordinates returned by {@link CaxtonFont#getAtlasLocation(int)}, the coordinates here have the <var>y</var>-axis pointing up; that is, higher <var>y</var>-coordinates correspond to points farther up.
     *
     * @param glyphId the ID of the glyph to retrieve the atlas location for
     * @return a packed bounding box for the glyph
     */
    public long getBbox(int glyphId) {
        if (glyphId < 0 || glyphId >= atlasSize) {
            throw new IndexOutOfBoundsException("i must be in [0, " + atlasSize + ") (got " + glyphId + ")");
        }
        return MemoryUtil.memGetLong(bboxes + 8 * ((long) glyphId));
    }

    /**
     * Get the location of the image data for the {@param i}th atlas page.
     *
     * @param i the index of the atlas page to retrieve
     * @return the address to the image data for the atlas page
     */
    public CaxtonAtlasTexture getAtlasPage(int i) {
        return this.pages[i];
    }

    /**
     * Returns a map from width values to glyph IDs.
     * <p>
     * In particular, the atlas bounding box width from {@link CaxtonFont#getAtlasLocation(int)} is used, not the value from {@link CaxtonFont#getBbox(int)}.
     *
     * @return a map from width values to glyph IDs
     */
    public Int2ObjectMap<IntList> getGlyphsByWidth() {
        return glyphsByWidth;
    }

    /**
     * Registers all atlas textures associated with this font.
     *
     * @param textureManager the {@link TextureManager} to which the textures should be registered
     */
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

    /**
     * Increments the reference count of this font.
     *
     * @return the receiver
     */
    public CaxtonFont cloneReference() {
        if (this.changes != null) {
            this.changes.add(Pair.of(Thread.currentThread().getStackTrace(), true));
        }
        ++this.refCount;
        return this;
    }

    /**
     * Constants for metric types.
     *
     * @see CaxtonFont#getMetrics(int)
     */
    public static class Metrics {
        public static int UNITS_PER_EM = 0;
        public static int ASCENDER = 1;
        public static int DESCENDER = 2;
        public static int HEIGHT = 3;
        public static int LINE_GAP = 4;
        public static int UNDERLINE_POSITION = 5;
        public static int UNDERLINE_THICKNESS = 6;
        public static int STRIKEOUT_POSITION = 7;
        public static int STRIKEOUT_THICKNESS = 8;
    }
}
