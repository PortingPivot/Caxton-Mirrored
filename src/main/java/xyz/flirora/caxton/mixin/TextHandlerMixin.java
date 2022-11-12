package xyz.flirora.caxton.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextHandler;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.flirora.caxton.render.CaxtonTextHandler;
import xyz.flirora.caxton.render.TextHandlerExt;

@Environment(EnvType.CLIENT)
@Mixin(TextHandler.class)
public class TextHandlerMixin implements TextHandlerExt {
    private CaxtonTextHandler caxtonTextHandler;

    @Override
    public void setCaxtonTextHandler(CaxtonTextHandler handler) {
        this.caxtonTextHandler = handler;
    }

    @Inject(at = @At("HEAD"), method = "getWidth(Ljava/lang/String;)F", cancellable = true)
    private void onGetWidth(String text, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(caxtonTextHandler.getWidth(text));
    }

    @Inject(at = @At("HEAD"), method = "getWidth(Lnet/minecraft/text/StringVisitable;)F", cancellable = true)
    private void onGetWidth(StringVisitable text, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(caxtonTextHandler.getWidth(text));
    }

    @Inject(at = @At("HEAD"), method = "getWidth(Lnet/minecraft/text/OrderedText;)F", cancellable = true)
    private void onGetWidth(OrderedText text, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(caxtonTextHandler.getWidth(text));
    }
}
