package xyz.flirora.caxton.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.NativeImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Environment(EnvType.CLIENT)
@Mixin(NativeImage.class)
public interface NativeImageAccessor {
    @Invoker("<init>")
    public static NativeImage callInit(NativeImage.Format format, int width, int height, boolean useStb, long pointer) {
        throw new UnsupportedOperationException("this is a mixin");
    }
}
