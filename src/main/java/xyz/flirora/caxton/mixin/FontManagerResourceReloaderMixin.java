package xyz.flirora.caxton.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.Font;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.flirora.caxton.font.CaxtonFontLoader;
import xyz.flirora.caxton.render.CaxtonTextRenderer;

import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
@Mixin(targets = {"net.minecraft.client.font.FontManager$1"})
public class FontManagerResourceReloaderMixin {
    // Clear font cache before reloading fonts.
    @Inject(at = @At("HEAD"), method = "prepare(Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/util/profiler/Profiler;)Ljava/util/Map;")
    private void onPrepare(ResourceManager resourceManager, Profiler profiler, CallbackInfoReturnable<Map<Identifier, List<Font>>> cir) {
        CaxtonFontLoader.clearFontCache();
        CaxtonTextRenderer.getInstance().clearCaches();
    }
}
