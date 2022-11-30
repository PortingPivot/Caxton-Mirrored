package xyz.flirora.caxton.font;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import xyz.flirora.caxton.dll.CaxtonInternal;
import xyz.flirora.caxton.mixin.NativeImageAccessor;

/**
 * A {@link NativeImageBackedTexture} that holds an atlas page for a {@link CaxtonFont}.
 * <p>
 * When this font is closed, its OpenGL texture handle is released, but the underlying image data is not deallocated as it is managed by the {@code Font} object in the Rust code.
 * <p>
 * Note that currently, glyphs are loaded eagerly into VRAM. This might change in the future but needs more consideration.
 */
@Environment(EnvType.CLIENT)
public class CaxtonAtlasTexture extends NativeImageBackedTexture {
    private final Identifier id;

    /**
     * Creates a new {@link CaxtonAtlasTexture}.
     * <p>
     * This does <strong>not</strong> give ownership to the memory pointed to by {@code fontPtr}.
     *
     * @param font    the parent {@link CaxtonFont} object
     * @param fontPtr the pointer kept by the {@link CaxtonFont} object to Rust’s {@code Font}
     * @param i       the index of the atlas page
     */
    protected CaxtonAtlasTexture(CaxtonFont font, long fontPtr, int i) {
        super(createNativeImage(fontPtr, font.getOptions().pageSize(), i));
        this.id = font.getId().withPath(path -> path + "/" + i);
    }

    private static NativeImage createNativeImage(long fontPtr, int size, int i) {
        long pageAddr = CaxtonInternal.fontAtlasPage(fontPtr, i);
        return NativeImageAccessor.callInit(
                NativeImage.Format.RGBA,
                size, size, false,
                pageAddr);
    }

    /**
     * Gets the {@link Identifier} associated with the texture.
     *
     * @return the {@link Identifier} associated with the texture
     */
    public Identifier getId() {
        return id;
    }

    /**
     * Closes this atlas texture, freeing its OpenGL ID but not deallocating the underlying image data.
     */
    @Override
    public void close() {
        // Do not close the underlying image; we do not own it
        this.clearGlId();
    }
}
