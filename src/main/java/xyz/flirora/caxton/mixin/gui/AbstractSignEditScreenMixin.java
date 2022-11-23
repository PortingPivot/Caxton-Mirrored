package xyz.flirora.caxton.mixin.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.client.render.*;
import net.minecraft.client.util.SelectionManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import xyz.flirora.caxton.layout.CaxtonText;
import xyz.flirora.caxton.layout.CaxtonTextHandler;
import xyz.flirora.caxton.layout.DirectionSetting;
import xyz.flirora.caxton.render.CaxtonTextRenderer;
import xyz.flirora.caxton.render.HasCaxtonTextRenderer;
import xyz.flirora.caxton.render.Voepfxo;

@Environment(EnvType.CLIENT)
@Mixin(AbstractSignEditScreen.class)
public abstract class AbstractSignEditScreenMixin
        extends Screen {
    @Shadow
    @Final
    protected SignBlockEntity blockEntity;
    @Shadow
    @Final
    protected String[] text;
    @Shadow
    private int ticksSinceOpened;
    @Shadow
    private SelectionManager selectionManager;
    @Shadow
    private int currentRow;

    protected AbstractSignEditScreenMixin(Text title) {
        super(title);
    }

    @Shadow
    protected abstract Vector3f getTextScale();

    /**
     * @author +merlan #flirora
     * @reason cursed
     */
    @Overwrite
    private void renderSignText(MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers) {
        CaxtonTextRenderer ctr = ((HasCaxtonTextRenderer) textRenderer).getCaxtonTextRenderer();
        CaxtonTextHandler cth = ctr.getHandler();

        matrices.translate(0.0f, 0.0f, 4.0f);
        Vector3f textScale = getTextScale();
        matrices.scale(textScale.x(), textScale.y(), textScale.z());

        int signColor = blockEntity.getTextColor().getSignColor();
        boolean showCursor = ticksSinceOpened / 6 % 2 == 0;
        int selectionStart = selectionManager.getSelectionStart();
        int selectionEnd = selectionManager.getSelectionEnd();
        int yAdj = 4 * this.blockEntity.getTextLineHeight() / 2;
        int lineY = currentRow * this.blockEntity.getTextLineHeight() - yAdj;
        Matrix4f positionMatrix = matrices.peek().getPositionMatrix();

        for (int row = 0; row < text.length; ++row) {
            String line = text[row];
            if (line == null) continue;

            CaxtonText ct = CaxtonText.fromFormatted(line, ctr.getFontStorageAccessor(), Style.EMPTY, false, this.textRenderer.isRightToLeft(), cth.getCache());

            float x = -cth.getWidth(ct) / 2.0f;
            ctr.draw(ct,
                    x, row * this.blockEntity.getTextLineHeight() - yAdj,
                    signColor, false,
                    positionMatrix, vertexConsumers,
                    false, 0, 0xF000F0, -1, Float.POSITIVE_INFINITY);
            if (row != this.currentRow || selectionStart < line.length() || !showCursor) continue;
            // Draw ending cursor
            float cursorX = -x;
            this.client.textRenderer.draw("_", cursorX, lineY, signColor, false, positionMatrix, vertexConsumers, false, 0, 0xF000F0, false);
        }
        vertexConsumers.draw();


        String currentLine = this.text[currentRow];
        if (currentLine != null && selectionStart >= 0) {
            CaxtonText ct = CaxtonText.fromFormatted(currentLine, ctr.getFontStorageAccessor(), Style.EMPTY, false, this.textRenderer.isRightToLeft(), cth.getCache());

            float x = cth.getWidth(ct) / 2;
            float cursorOffset = cth.getOffsetAtIndex(ct, Math.min(selectionStart, currentLine.length()), DirectionSetting.AUTO);
            float cursorX = cursorOffset - x;
            if (showCursor && selectionStart < currentLine.length()) {
                Voepfxo.fill(
                        matrices,
                        cursorX, lineY - 1,
                        cursorX + 1, lineY + this.blockEntity.getTextLineHeight(),
                        0xFF000000 | signColor);
            }
            if (selectionEnd == selectionStart) return;

            int selectionMin = Math.min(selectionStart, selectionEnd);
            int selectionMax = Math.max(selectionStart, selectionEnd);

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferBuilder = tessellator.getBuffer();
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            RenderSystem.disableTexture();
            RenderSystem.enableColorLogicOp();
            RenderSystem.logicOp(GlStateManager.LogicOp.OR_REVERSE);
            bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            cth.getHighlightRanges(ct, selectionMin, selectionMax, (start, end) -> {
                bufferBuilder.vertex(positionMatrix, start - x, lineY + this.blockEntity.getTextLineHeight(), 0.0f)
                        .color(0, 0, 255, 255)
                        .next();
                bufferBuilder.vertex(positionMatrix, end - x, lineY + this.blockEntity.getTextLineHeight(), 0.0f)
                        .color(0, 0, 255, 255)
                        .next();
                bufferBuilder.vertex(positionMatrix, end - x, lineY, 0.0f)
                        .color(0, 0, 255, 255)
                        .next();
                bufferBuilder.vertex(positionMatrix, start - x, lineY, 0.0f)
                        .color(0, 0, 255, 255)
                        .next();
            });
            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
            RenderSystem.disableColorLogicOp();
            RenderSystem.enableTexture();
        }
    }
}
