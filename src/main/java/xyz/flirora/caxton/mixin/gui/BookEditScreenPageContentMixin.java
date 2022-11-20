package xyz.flirora.caxton.mixin.gui;

import com.google.common.collect.ImmutableList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.flirora.caxton.layout.CaxtonText;
import xyz.flirora.caxton.layout.ForwardTraversedMap;
import xyz.flirora.caxton.layout.gui.BookEditScreenPageContentExt;

import java.util.List;

@Environment(EnvType.CLIENT)
@Mixin(BookEditScreen.PageContent.class)
public class BookEditScreenPageContentMixin implements BookEditScreenPageContentExt {
    @Shadow
    @Final
    static BookEditScreen.PageContent EMPTY;
    private List<CaxtonText> caxtonTexts;
    private ForwardTraversedMap warts;

    @Inject(at = @At("RETURN"), method = "<clinit>")
    private static void onStaticInit(CallbackInfo ci) {
        ((BookEditScreenPageContentExt) EMPTY).setCaxtonText(ImmutableList.of(CaxtonText.EMPTY));
        ForwardTraversedMap warts = new ForwardTraversedMap();
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
    public ForwardTraversedMap getWarts() {
        return warts;
    }

    @Override
    public void setWarts(ForwardTraversedMap warts) {
        this.warts = warts;
    }
}
