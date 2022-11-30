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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.flirora.caxton.CaxtonModClient;
import xyz.flirora.caxton.layout.CaxtonTextHandler;
import xyz.flirora.caxton.layout.TextHandlerExt;

import java.util.function.BiConsumer;

@Environment(EnvType.CLIENT)
@Mixin(TextHandler.class)
public class TextHandlerMixin implements TextHandlerExt {
    private static final Logger LOGGER = LogUtils.getLogger();
    private CaxtonTextHandler caxtonTextHandler;

    @Override
    public CaxtonTextHandler getCaxtonTextHandler() {
        return caxtonTextHandler;
    }

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
        CaxtonModClient.onBrokenMethod();
    }

    @Inject(at = @At("HEAD"), method = "trimToWidthBackwards")
    private void onTrimToWidthBackwards(String text, int maxWidth, Style style, CallbackInfoReturnable<String> cir) {
        CaxtonModClient.onBrokenMethod();
    }

    @Inject(at = @At("HEAD"), method = "getLimitedStringLength", cancellable = true)
    private void onGetLimitedStringLength(String text, int maxWidth, Style style, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(caxtonTextHandler.getCharIndexAtXFormatted(text, maxWidth, style));
    }

    @Inject(at = @At("HEAD"), method = "limitString")
    private void onLimitString(String text, int maxWidth, Style style, CallbackInfoReturnable<String> cir) {
        CaxtonModClient.onBrokenMethod();
    }

    // Surprisingly, this method is never misused as egregiously as its String overload in Minecraft.
    // However, appending ellipses to the result will pose some problems.
    // For instance, imagine that capital letters were written right to left in English.
    // Suppose we want to call this method on the following (given in visual order):
    // you are a NOSREP ECIN
    //              | ← trim point
    // Should the result be “you are a CIN…” or “you are a NOS…”, or perhaps
    // “you are a …CIN”? (It would probably be the third one, actually.)
    @Inject(at = @At("HEAD"), method = "trimToWidth(Lnet/minecraft/text/StringVisitable;ILnet/minecraft/text/Style;)Lnet/minecraft/text/StringVisitable;", cancellable = true)
    private void onTrimToWidth(StringVisitable text, int width, Style style, CallbackInfoReturnable<StringVisitable> cir) {
        cir.setReturnValue(caxtonTextHandler.trimToWidth(text, width, style));
    }

    @Inject(at = @At("HEAD"), method = "getStyleAt(Lnet/minecraft/text/StringVisitable;I)Lnet/minecraft/text/Style;", cancellable = true)
    private void onGetStyleAt(StringVisitable text, int x, CallbackInfoReturnable<Style> cir) {
        cir.setReturnValue(caxtonTextHandler.getStyleAt(text, x));
    }

    @Inject(at = @At("HEAD"), method = "getStyleAt(Lnet/minecraft/text/OrderedText;I)Lnet/minecraft/text/Style;", cancellable = true)
    private void onGetStyleAt(OrderedText text, int x, CallbackInfoReturnable<Style> cir) {
        cir.setReturnValue(caxtonTextHandler.getStyleAt(text, x));
    }

    @Inject(at = @At("HEAD"), method = "wrapLines(Lnet/minecraft/text/StringVisitable;ILnet/minecraft/text/Style;Ljava/util/function/BiConsumer;)V", cancellable = true)
    private void onWrapLines(StringVisitable text, int maxWidth, Style style, BiConsumer<StringVisitable, Boolean> lineConsumer, CallbackInfo ci) {
        caxtonTextHandler.wrapLines(text, maxWidth, style, lineConsumer);
        ci.cancel();
    }

    @Inject(at = @At("HEAD"), method = "wrapLines(Ljava/lang/String;ILnet/minecraft/text/Style;ZLnet/minecraft/client/font/TextHandler$LineWrappingConsumer;)V", cancellable = true)
    private void onWrapLines(String text, int maxWidth, Style style, boolean retainTrailingWordSplit, TextHandler.LineWrappingConsumer consumer, CallbackInfo ci) {
        caxtonTextHandler.wrapLines(text, maxWidth, style, retainTrailingWordSplit, consumer);
        ci.cancel();
    }
}
