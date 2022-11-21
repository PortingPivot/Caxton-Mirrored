package xyz.flirora.caxton.mixin.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.flirora.caxton.layout.gui.TextFieldWidgetExt;

@Environment(EnvType.CLIENT)
@Mixin(ChatInputSuggestor.class)
public class ChatInputSuggestorMixin {
    @Shadow
    @Final
    TextFieldWidget textField;

    @Inject(at = @At("RETURN"), method = "refresh")
    private void afterRefresh(CallbackInfo ci) {
        ((TextFieldWidgetExt) textField).updateCaxtonText();
    }
}
