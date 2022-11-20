package xyz.flirora.caxton.mixin.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import xyz.flirora.caxton.layout.CaxtonText;
import xyz.flirora.caxton.layout.gui.BookEditScreenPageContentExt;

import java.util.List;

@Environment(EnvType.CLIENT)
@Mixin(BookEditScreen.PageContent.class)
public class BookEditScreenPageContentMixin implements BookEditScreenPageContentExt {
    private List<CaxtonText> caxtonTexts;

    @Override
    public List<CaxtonText> getCaxtonText() {
        return caxtonTexts;
    }

    @Override
    public void setCaxtonText(List<CaxtonText> texts) {
        this.caxtonTexts = texts;
    }
}
