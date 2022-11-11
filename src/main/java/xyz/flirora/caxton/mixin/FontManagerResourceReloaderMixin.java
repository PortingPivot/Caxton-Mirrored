package xyz.flirora.caxton.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.Font;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.flirora.caxton.client.CaxtonModClient;
import xyz.flirora.caxton.font.CaxtonFontLoader;
import xyz.flirora.caxton.font.CaxtonTextRenderer;

import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
@Mixin(targets = {"net.minecraft.client.font.FontManager$1"})
public class FontManagerResourceReloaderMixin {
    // Test text layout; runs whenever the fonts are reloaded.
    @Inject(at = @At("TAIL"), method = "apply(Ljava/util/Map;Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/util/profiler/Profiler;)V")
    private void afterApply(CallbackInfo ci) {
        CaxtonModClient.testRunLister(MinecraftClient.getInstance());
    }

    // Clear font cache before reloading fonts.
    @Inject(at = @At("HEAD"), method = "prepare(Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/util/profiler/Profiler;)Ljava/util/Map;")
    private void onPrepare(ResourceManager resourceManager, Profiler profiler, CallbackInfoReturnable<Map<Identifier, List<Font>>> cir) {
        CaxtonFontLoader.clearFontCache();
        CaxtonTextRenderer.getInstance().clearCaches();
    }
}
