package xyz.flirora.caxton.mixin.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.flirora.caxton.layout.CaxtonText;
import xyz.flirora.caxton.layout.DirectionSetting;
import xyz.flirora.caxton.layout.gui.TextFieldWidgetExt;
import xyz.flirora.caxton.mixin.TextRendererAccessor;
import xyz.flirora.caxton.render.CaxtonTextRenderer;
import xyz.flirora.caxton.render.HasCaxtonTextRenderer;

import java.util.function.BiFunction;

@Environment(EnvType.CLIENT)
@Mixin(TextFieldWidget.class)
public abstract class TextFieldWidgetMixin extends ClickableWidget
        implements Drawable,
        Element, TextFieldWidgetExt {
    private static final Style SUGGESTION = Style.EMPTY.withColor(0xFF808080);
    @Shadow
    @Final
    private static String HORIZONTAL_CURSOR;
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
    @Shadow
    private int firstCharacterIndex;
    private CaxtonText caxtonText;
    @Shadow
    private int selectionStart;
    @Shadow
    private int selectionEnd;
    @Shadow
    private BiFunction<String, Integer, OrderedText> renderTextProvider;
    @Shadow
    private @Nullable String suggestion;

    public TextFieldWidgetMixin(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
    }

    @Shadow
    public abstract int getInnerWidth();

    @Shadow
    protected abstract int getMaxLength();

    @Shadow
    public abstract void setCursor(int cursor);

    public void updateCaxtonText() {
        boolean hideSuggestion = selectionStart < text.length() || text.length() >= getMaxLength();
        OrderedText text = renderTextProvider.apply(this.text, 0);
        if (!hideSuggestion && suggestion != null) {
            text = OrderedText.concat(text, OrderedText.styledForwardsVisitedString(suggestion, SUGGESTION));
        }

        this.caxtonText = CaxtonText.from(text, ((TextRendererAccessor) textRenderer).getFontStorageAccessor(), true, false, ((HasCaxtonTextRenderer) textRenderer).getCaxtonTextRenderer().getHandler().getCache());
//        System.err.println("caxtonText = " + this.caxtonText);
    }

    // We can’t inject on `onChanged`, as some methods that update `text`
    // call `setSelectionEnd` before `onChanged`.
    @Inject(at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;text:Ljava/lang/String;", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER), method = "setText")
    private void updateCaxtonTextOnSetText(String text, CallbackInfo ci) {
        updateCaxtonText();
    }

    @Inject(at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;text:Ljava/lang/String;", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER), method = "write")
    private void updateCaxtonTextOnWrite(String text, CallbackInfo ci) {
        updateCaxtonText();
    }

    @Inject(at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;text:Ljava/lang/String;", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER), method = "eraseCharacters")
    private void updateCaxtonTextOnEraseCharacters(int maxLength, CallbackInfo ci) {
        updateCaxtonText();
    }

    @Inject(at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;text:Ljava/lang/String;", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER), method = "setMaxLength")
    private void updateCaxtonTextOnSetMaxLength(int maxLength, CallbackInfo ci) {
        updateCaxtonText();
    }

    @Inject(at = @At("TAIL"), method = "setSuggestion")
    private void updateCaxtonTextOnSuggestionSet(String text, CallbackInfo ci) {
        updateCaxtonText();
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;getInnerWidth()I"), method = "renderButton", cancellable = true)
    private void onRenderButton(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        CaxtonTextRenderer ctr = ((HasCaxtonTextRenderer) textRenderer).getCaxtonTextRenderer();

        int color = editable ? editableColor : uneditableColor;
        int x = drawsBackground ? getX() + 4 : getX();
        int y = drawsBackground ? getY() + (height - 8) / 2 : getY();

        float firstCharLocation = 0, selectionStartLocation = 0;

        if (caxtonText != null) {
            VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());

            ctr.draw(
                    caxtonText,
                    x, y,
                    color, true,
                    matrices.peek().getPositionMatrix(), immediate,
                    false, 0, 0xF000F0,
                    firstCharacterIndex, getInnerWidth());
            firstCharLocation = ctr.getHandler().getOffsetAtIndex(caxtonText, firstCharacterIndex, DirectionSetting.FORCE_LTR);
            selectionStartLocation = ctr.getHandler().getOffsetAtIndex(caxtonText, selectionStart, DirectionSetting.AUTO);

            immediate.draw();
        }

        boolean cursorIsVertical = selectionStart < text.length() || text.length() >= getMaxLength();

        float cursorLocation = selectionStartLocation - firstCharLocation;

        boolean cursorInBounds = 0 <= cursorLocation && cursorLocation < getInnerWidth();
        boolean showCursor = isFocused() && focusedTicks / 6 % 2 == 0 && cursorInBounds;

        int cursorX = Math.round(x + cursorLocation);

        if (!cursorInBounds) {
            cursorX = cursorLocation > 0 ? x + width : x;
        }

        if (showCursor) {
            if (cursorIsVertical) {
                DrawableHelper.fill(matrices, cursorX, y - 1, cursorX + 1, y + 1 + this.textRenderer.fontHeight, 0xFFD0D0D0);
            } else {
                textRenderer.drawWithShadow(matrices, HORIZONTAL_CURSOR, cursorX, y, color);
            }
        }

        if (selectionStart != selectionEnd) {
            float finalFirstCharLocation = firstCharLocation;
            ctr.getHandler().getHighlightRanges(
                    caxtonText,
                    Math.min(selectionStart, selectionEnd),
                    Math.max(selectionStart, selectionEnd),
                    (x0, x1) -> myDrawSelectionHighlight(
                            x + x0 - finalFirstCharLocation, y - 1,
                            x + x1 - finalFirstCharLocation, y + 1 + textRenderer.fontHeight));
        }

        // Ḋo.
        ci.cancel();
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;getInnerWidth()I"), method = "mouseClicked", cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (caxtonText == null) {
            cir.setReturnValue(false);
            return;
        }

        CaxtonTextRenderer ctr = ((HasCaxtonTextRenderer) textRenderer).getCaxtonTextRenderer();

        float xOffset = (float) (mouseX - this.getX());
        if (this.drawsBackground) {
            xOffset -= 4;
        }

        int index = ctr.getHandler().getCharIndexAtX(caxtonText, xOffset, firstCharacterIndex);

        this.setCursor(index);
        cir.setReturnValue(true);
    }

    @Inject(at = @At("HEAD"), method = "setSelectionEnd", cancellable = true)
    private void onSetSelectionEnd(int index, CallbackInfo ci) {
        int length = this.text.length();
        this.selectionEnd = MathHelper.clamp(index, 0, length);

        if (this.textRenderer != null && this.caxtonText != null) {
            if (this.firstCharacterIndex > length) {
                this.firstCharacterIndex = length;
            }

            int width = getInnerWidth();

            CaxtonTextRenderer ctr = ((HasCaxtonTextRenderer) textRenderer).getCaxtonTextRenderer();

            float firstCharLocation = ctr.getHandler().getOffsetAtIndex(caxtonText, firstCharacterIndex, DirectionSetting.FORCE_LTR);
            float selectionEndLocation = ctr.getHandler().getOffsetAtIndex(caxtonText, selectionEnd, DirectionSetting.AUTO);
//            System.out.println("firstCharacterIndex = " + firstCharacterIndex + ", selectionEnd = " + selectionEnd);
//            System.out.println("firstCharLocation = " + firstCharLocation + ", selectionEndLocation = " + selectionEndLocation);

            if (selectionEndLocation < firstCharLocation) {
                // Update firstCharacterIndex to point to a char farther to the left
                this.firstCharacterIndex = ctr.getHandler().getCharIndexAtX(caxtonText, firstCharLocation - width, -1);
            } else if (selectionEndLocation >= firstCharLocation + width) {
                // Update firstCharacterIndex to point to a char farther to the right
                this.firstCharacterIndex = ctr.getHandler().getCharIndexAfterX(caxtonText, selectionEndLocation - getInnerWidth(), -1);
            }

            this.firstCharacterIndex = MathHelper.clamp(this.firstCharacterIndex, 0, length);
        }

        ci.cancel();
    }

    // Copy of drawSelectionHighlight with float arguments
    private void myDrawSelectionHighlight(float x1, float y1, float x2, float y2) {
        float i;
        if (x1 < x2) {
            i = x1;
            x1 = x2;
            x2 = i;
        }
        if (y1 < y2) {
            i = y1;
            y1 = y2;
            y2 = i;
        }
        if (x2 > this.getX() + this.width) {
            x2 = this.getX() + this.width;
        }
        if (x1 > this.getX() + this.width) {
            x1 = this.getX() + this.width;
        }
        if (x2 < this.getX()) {
            x2 = this.getX();
        }
        if (x1 < this.getX()) {
            x1 = this.getX();
        }
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        RenderSystem.setShader(GameRenderer::getPositionProgram);
        RenderSystem.setShaderColor(0.0f, 0.0f, 1.0f, 1.0f);
        RenderSystem.disableTexture();
        RenderSystem.enableColorLogicOp();
        RenderSystem.logicOp(GlStateManager.LogicOp.OR_REVERSE);
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        bufferBuilder.vertex(x1, y2, 0.0).next();
        bufferBuilder.vertex(x2, y2, 0.0).next();
        bufferBuilder.vertex(x2, y1, 0.0).next();
        bufferBuilder.vertex(x1, y1, 0.0).next();
        tessellator.draw();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableColorLogicOp();
        RenderSystem.enableTexture();
    }
}
