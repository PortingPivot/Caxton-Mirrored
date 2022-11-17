package xyz.flirora.caxton.mixin;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextHandler;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.flirora.caxton.CaxtonMod;
import xyz.flirora.caxton.render.CaxtonTextHandler;
import xyz.flirora.caxton.render.TextHandlerExt;

@Environment(EnvType.CLIENT)
@Mixin(TextHandler.class)
public class TextHandlerMixin implements TextHandlerExt {
    private static final Logger LOGGER = LogUtils.getLogger();
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

    @Inject(at = @At("HEAD"), method = "getTrimmedLength", cancellable = true)
    private void onGetTrimmedLength(String text, int maxWidth, Style style, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(caxtonTextHandler.getCharIndexAtX(text, maxWidth, style));
    }

    @Inject(at = @At("HEAD"), method = "trimToWidth(Ljava/lang/String;ILnet/minecraft/text/Style;)Ljava/lang/String;")
    private void onTrimToWidth(String text, int maxWidth, Style style, CallbackInfoReturnable<String> cir) {
        CaxtonMod.onBrokenMethod();
    }

    @Inject(at = @At("HEAD"), method = "trimToWidthBackwards")
    private void onTrimToWidthBackwards(String text, int maxWidth, Style style, CallbackInfoReturnable<String> cir) {
        // TODO: reimplement
        CaxtonMod.onBrokenMethod();
    }

    @Inject(at = @At("HEAD"), method = "getLimitedStringLength", cancellable = true)
    private void onGetLimitedStringLength(String text, int maxWidth, Style style, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(caxtonTextHandler.getCharIndexAtXFormatted(text, maxWidth, style));
    }

    @Inject(at = @At("HEAD"), method = "limitString")
    private void onLimitString(String text, int maxWidth, Style style, CallbackInfoReturnable<String> cir) {
        CaxtonMod.onBrokenMethod();
    }

    @Inject(at = @At("HEAD"), method = "trimToWidth(Lnet/minecraft/text/StringVisitable;ILnet/minecraft/text/Style;)Lnet/minecraft/text/StringVisitable;")
    private void onTrimToWidth(StringVisitable text, int width, Style style, CallbackInfoReturnable<StringVisitable> cir) {
        // TODO: reimplement
        CaxtonMod.onBrokenMethod();
    }
}
