package xyz.flirora.caxton.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import xyz.flirora.caxton.render.WorldRendererVertexConsumerProvider;

import java.util.Map;

@Environment(EnvType.CLIENT)
@Mixin(BufferBuilderStorage.class)
public class BufferBuilderStorageMixin {
    @Redirect(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/VertexConsumerProvider;immediate(Ljava/util/Map;Lnet/minecraft/client/render/BufferBuilder;)Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;"),
            slice = @Slice(
                    from = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/client/render/BufferBuilderStorage;entityBuilders:Ljava/util/SortedMap;"),
                    to = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/client/render/BufferBuilderStorage;entityVertexConsumers:Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;")),
            allow = 1)
    private VertexConsumerProvider.Immediate redirect(Map<RenderLayer, BufferBuilder> layerBuffers, BufferBuilder fallbackBuffer) {
//        return VertexConsumerProvider.immediate(layerBuffers, fallbackBuffer);
        return new WorldRendererVertexConsumerProvider(fallbackBuffer, layerBuffers);
    }
}
