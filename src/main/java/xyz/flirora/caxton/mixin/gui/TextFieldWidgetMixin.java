package xyz.flirora.caxton.mixin.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(TextFieldWidget.class)
public class TextFieldWidgetMixin {
    @Inject(at = @At("HEAD"), method = "renderButton")
    private void onRenderButton(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        //
    }

    @Inject(at = @At("HEAD"), method = "mouseClicked")
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        //
    }

    @Inject(at = @At("HEAD"), method = "setSelectionEnd")
    private void onSetSelectionEnd(int index, CallbackInfo ci) {
        //
    }
}
