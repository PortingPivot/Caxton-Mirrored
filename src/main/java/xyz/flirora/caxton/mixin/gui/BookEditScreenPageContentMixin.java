package xyz.flirora.caxton.mixin.gui;

import com.google.common.collect.ImmutableList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.flirora.caxton.layout.CaxtonText;
import xyz.flirora.caxton.layout.CaxtonTextHandler;
import xyz.flirora.caxton.layout.FcIndexConverter;
import xyz.flirora.caxton.layout.gui.BookEditScreenPageContentExt;
import xyz.flirora.caxton.render.CaxtonTextRenderer;
import xyz.flirora.caxton.render.HasCaxtonTextRenderer;

import java.util.List;

@Environment(EnvType.CLIENT)
@Mixin(BookEditScreen.PageContent.class)
public class BookEditScreenPageContentMixin implements BookEditScreenPageContentExt {
    @Shadow
    @Final
    static BookEditScreen.PageContent EMPTY;
    @Shadow
    @Final
    private int[] lineStarts;
    @Shadow
    @Final
    private BookEditScreen.Line[] lines;
    @Shadow
    @Final
    private String pageContent;
    private List<CaxtonText> caxtonTexts;
    private FcIndexConverter warts;

    @Inject(at = @At("RETURN"), method = "<clinit>")
    private static void onStaticInit(CallbackInfo ci) {
        ((BookEditScreenPageContentExt) EMPTY).setCaxtonText(ImmutableList.of(CaxtonText.EMPTY));
        FcIndexConverter warts = new FcIndexConverter();
        warts.put(Integer.MIN_VALUE, 0);
        ((BookEditScreenPageContentExt) EMPTY).setWarts(warts);
    }

    @Override
    public List<CaxtonText> getCaxtonText() {
        return caxtonTexts;
    }

    @Override
    public void setCaxtonText(List<CaxtonText> texts) {
        this.caxtonTexts = texts;
    }

    @Override
    public FcIndexConverter getWarts() {
        return warts;
    }

    @Override
    public void setWarts(FcIndexConverter warts) {
        this.warts = warts;
    }

    @Inject(at = @At("HEAD"), method = "getCursorPosition", cancellable = true)
    private void tweakCursorPosition(TextRenderer renderer, BookEditScreen.Position position, CallbackInfoReturnable<Integer> cir) {
        CaxtonTextRenderer ctr = ((HasCaxtonTextRenderer) renderer).getCaxtonTextRenderer();
        CaxtonTextHandler cth = ctr.getHandler();

        int lineNum = position.y / renderer.fontHeight;
        if (lineNum < 0) {
            cir.setReturnValue(0);
            return;
        }
        if (lineNum >= lines.length) {
            cir.setReturnValue(pageContent.length());
            return;
        }
        int start = this.lineStarts[lineNum];
        int a = warts.formatfulToFormatless(start, true);
        int b = cth.getCharIndexAtX(caxtonTexts.get(lineNum), position.x, 0);
        cir.setReturnValue(warts.formatlessToFormatful(a + b));
//        System.err.println("start = " + start + ", a = " + a + ", b = " + b + ", result = " + cir.getReturnValue());
        warts.reset();
    }
}
