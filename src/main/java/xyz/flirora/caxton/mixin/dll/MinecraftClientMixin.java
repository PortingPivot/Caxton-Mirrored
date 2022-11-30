package xyz.flirora.caxton.mixin.dll;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.flirora.caxton.dll.LibraryLoading;

@Environment(EnvType.CLIENT)
@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(at = @At("RETURN"), method = "<init>")
    private void onInit(RunArgs args, CallbackInfo ci) {
        Exception e = LibraryLoading.getLoadingException();
        if (e != null) {
            LibraryLoading.showNativeLibraryLoadFailedScreen((MinecraftClient) (Object) this, e);
        }
    }
}
