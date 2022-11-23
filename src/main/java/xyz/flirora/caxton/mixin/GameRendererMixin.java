package xyz.flirora.caxton.mixin;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.resource.ResourceFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.flirora.caxton.render.CaxtonShaders;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "preloadPrograms", at = @At(value = "RETURN"))
    private void afterPreloadShaders(ResourceFactory factory, CallbackInfo ci) {
//        CaxtonShaders.caxtonTextShader = this.loadShader(factory, "caxton_rendertype_text", VertexFormats.POSITION_COLOR_TEXTURE_LIGHT);
    }

    @ModifyReceiver(method = "loadPrograms(Lnet/minecraft/resource/ResourceFactory;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 0))
    private List<Pair<ShaderProgram, Consumer<ShaderProgram>>> addMyShaders(
            List<Pair<ShaderProgram, Consumer<ShaderProgram>>> shaders,
            Object nextShader,
            ResourceFactory factory) throws IOException {
        shaders.add(Pair.of(new ShaderProgram(factory, "caxton_rendertype_text", VertexFormats.POSITION_COLOR_TEXTURE_LIGHT), shader -> {
            CaxtonShaders.caxtonTextShader = shader;
        }));
        shaders.add(Pair.of(new ShaderProgram(factory, "caxton_rendertype_text_see_through", VertexFormats.POSITION_COLOR_TEXTURE_LIGHT), shader -> {
            CaxtonShaders.caxtonTextSeeThroughShader = shader;
        }));
        return shaders;
    }
}
