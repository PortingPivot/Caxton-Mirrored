package xyz.flirora.caxton.font;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import xyz.flirora.caxton.mixin.NativeImageAccessor;

@Environment(EnvType.CLIENT)
public class CaxtonAtlasTexture extends NativeImageBackedTexture {
    private final Identifier id;

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

    public Identifier getId() {
        return id;
    }

    @Override
    public void close() {
        // Do not close the underlying image; we do not own it
        this.clearGlId();
    }
}
