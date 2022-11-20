package xyz.flirora.caxton.mixin.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin(BookEditScreen.PageContent.class)
public interface BookEditScreenPageContentAccessor {
    @Accessor("EMPTY")
    static BookEditScreen.PageContent getEmpty() {
        return null;
    }

    @Accessor
    BookEditScreen.Line[] getLines();
}
