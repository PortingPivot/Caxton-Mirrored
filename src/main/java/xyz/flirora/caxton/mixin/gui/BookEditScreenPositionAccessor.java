package xyz.flirora.caxton.mixin.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Environment(EnvType.CLIENT)
@Mixin(BookEditScreen.Position.class)
public interface BookEditScreenPositionAccessor {
    @Invoker("<init>")
    static BookEditScreen.Position callInit(int x, int y) {
        return null;
    }
}
