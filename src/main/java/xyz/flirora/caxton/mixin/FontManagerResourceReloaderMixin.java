package xyz.flirora.caxton.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.flirora.caxton.client.CaxtonModClient;

// Mixin for testing text layout; runs whenever the fonts are reloaded.
@Environment(EnvType.CLIENT)
@Mixin(targets = {"net.minecraft.client.font.FontManager$1"})
public class FontManagerResourceReloaderMixin {
    @Inject(at = @At("TAIL"), method = "apply(Ljava/util/Map;Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/util/profiler/Profiler;)V")
    private void afterApply(CallbackInfo ci) {
        CaxtonModClient.testRunLister(MinecraftClient.getInstance());
    }
}
