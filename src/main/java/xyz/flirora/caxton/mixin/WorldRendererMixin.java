package xyz.flirora.caxton.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import xyz.flirora.caxton.render.WorldRendererVertexConsumerProvider;

@Environment(EnvType.CLIENT)
@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;draw(Lnet/minecraft/client/render/RenderLayer;)V",
                    ordinal = 0),
            slice = @Slice(
                    from = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/RenderLayer;getWaterMask()Lnet/minecraft/client/render/RenderLayer;")
            ))
    private void addTranslucentDraws(VertexConsumerProvider.Immediate immediate, RenderLayer layer) {
        immediate.draw(layer);
        ((WorldRendererVertexConsumerProvider) immediate).drawCaxtonTextLayers();
    }
}
