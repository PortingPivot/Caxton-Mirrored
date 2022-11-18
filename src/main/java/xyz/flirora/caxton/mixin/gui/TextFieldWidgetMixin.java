package xyz.flirora.caxton.mixin.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.flirora.caxton.font.CaxtonText;
import xyz.flirora.caxton.mixin.TextRendererAccessor;
import xyz.flirora.caxton.render.HasCaxtonTextRenderer;

@Environment(EnvType.CLIENT)
@Mixin(TextFieldWidget.class)
public abstract class TextFieldWidgetMixin extends ClickableWidget
        implements Drawable,
        Element {
    @Shadow
    @Final
    private TextRenderer textRenderer;
    @Shadow
    private String text;
    @Shadow
    private boolean editable;
    @Shadow
    private int editableColor;
    @Shadow
    private int uneditableColor;
    @Shadow
    private int focusedTicks;
    @Shadow
    private boolean drawsBackground;
    private CaxtonText caxtonText;

    public TextFieldWidgetMixin(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
    }

    private void updateCaxtonText() {
        this.caxtonText = CaxtonText.fromForwards(text, ((TextRendererAccessor) textRenderer).getFontStorageAccessor(), Style.EMPTY, true, false, ((HasCaxtonTextRenderer) textRenderer).getCaxtonTextRenderer().getHandler().getCache());
        System.err.println("caxtonText = " + this.caxtonText);
    }

    // onChanged fires even when only the selection positions change. If this
    // adds too much overhead, then we can use more fine-grained injections later.
    @Inject(at = @At("TAIL"), method = "onChanged")
    private void updateCaxtonTextOnChange(String text, CallbackInfo ci) {
        updateCaxtonText();
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;getInnerWidth()I"), method = "renderButton", cancellable = true)
    private void onRenderButton(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        int color = editable ? editableColor : uneditableColor;
        int x = drawsBackground ? getX() + 4 : getX();
        int y = drawsBackground ? getY() + (height - 8) / 2 : getY();
        int x2 = x;
        boolean cursorInBounds = true;
        boolean showCursor = isFocused() && focusedTicks / 6 % 2 == 0 && cursorInBounds;
        // Ḋo.
        ci.cancel();
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
