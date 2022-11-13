package xyz.flirora.caxton.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.text.OrderedText;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.flirora.caxton.render.CaxtonTextRenderer;
import xyz.flirora.caxton.render.HasCaxtonTextRenderer;

import java.util.function.Function;

@Environment(EnvType.CLIENT)
@Mixin(TextRenderer.class)
public class TextRendererMixin implements HasCaxtonTextRenderer {
    private CaxtonTextRenderer caxtonRenderer;

    @Inject(at = @At("TAIL"), method = "<init>(Ljava/util/function/Function;Z)V")
    private void onInit(Function<Identifier, FontStorage> fontStorageAccessor, boolean validateAdvance, CallbackInfo ci) {
        this.caxtonRenderer = new CaxtonTextRenderer(fontStorageAccessor, (TextRenderer) (Object) this);
    }

    // Stub out TextRenderer#mirror call; we will call this on each
    // legacy run instead.
    // Note that we can’t simply ask rustybuzz to shape text as if it were
    // left-to-right; see <https://harfbuzz.github.io/harfbuzz-hb-buffer.html#hb-buffer-set-direction>
    // for more details.
    @Redirect(method = "drawInternal(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;ZIIZ)I", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/TextRenderer;mirror(Ljava/lang/String;)Ljava/lang/String;"))
    private String onMirror(TextRenderer instance, String text) {
        caxtonRenderer.rtl = true;
        return text;
    }

    @Inject(at = @At("HEAD"), method = "drawLayer(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;ZII)F", cancellable = true)
    private void onDrawLayerString(String text, float x, float y, int color, boolean shadow, Matrix4f matrix, VertexConsumerProvider vertexConsumerProvider, boolean seeThrough, int underlineColor, int light, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(caxtonRenderer.drawLayer(text, x, y, color, shadow, matrix, vertexConsumerProvider, seeThrough, underlineColor, light));
    }

    @Inject(at = @At("HEAD"), method = "drawLayer(Lnet/minecraft/text/OrderedText;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;ZII)F", cancellable = true)
    private void onDrawLayerOrderedText(OrderedText text, float x, float y, int color, boolean shadow, Matrix4f matrix, VertexConsumerProvider vertexConsumerProvider, boolean seeThrough, int underlineColor, int light, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(caxtonRenderer.drawLayer(text, x, y, color, shadow, matrix, vertexConsumerProvider, seeThrough, underlineColor, light));
    }

    @Override
    public CaxtonTextRenderer getCaxtonTextRenderer() {
        return caxtonRenderer;
    }
}
