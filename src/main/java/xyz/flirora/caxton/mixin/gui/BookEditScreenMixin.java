package xyz.flirora.caxton.mixin.gui;

import com.google.common.collect.Lists;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextHandler;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.SelectionManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import xyz.flirora.caxton.layout.CaxtonText;
import xyz.flirora.caxton.layout.CaxtonTextHandler;
import xyz.flirora.caxton.layout.DirectionSetting;
import xyz.flirora.caxton.layout.FcIndexConverter;
import xyz.flirora.caxton.layout.gui.BookEditScreenPageContentExt;
import xyz.flirora.caxton.render.CaxtonTextRenderer;
import xyz.flirora.caxton.render.HasCaxtonTextRenderer;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
@Mixin(BookEditScreen.class)
public abstract class BookEditScreenMixin extends Screen {
    private static final float XSCALE = 16.0f;
    @Shadow
    @Final
    private SelectionManager currentPageSelectionManager;
    private int charIndexOfCursor;

    protected BookEditScreenMixin(Text title) {
        super(title);
    }

    @Shadow
    static int getLineFromOffset(int[] lineStarts, int position) {
        return 0;
    }

    @Shadow
    protected abstract String getCurrentPageContent();

    @Shadow
    protected abstract Rect2i getLineSelectionRectangle(String string, TextHandler handler, int selectionStart, int selectionEnd, int lineY, int lineStart);

    @Shadow
    protected abstract Rect2i getRectFromCorners(BookEditScreen.Position start, BookEditScreen.Position end);

    @Shadow
    protected abstract BookEditScreen.Position absolutePositionToScreenPosition(BookEditScreen.Position position);

    /**
     * @author +merlan #flirora
     * @reason This method requires major changes to relax the assumptions that it makes.
     */
    @Overwrite
    private BookEditScreen.PageContent createPageContent() {
        CaxtonTextRenderer ctr = ((HasCaxtonTextRenderer) textRenderer).getCaxtonTextRenderer();
        CaxtonTextHandler cth = ctr.getHandler();

        String content = this.getCurrentPageContent();
        if (content.isEmpty()) {
            return BookEditScreenPageContentAccessor.getEmpty();
        }

        int selectionStart = this.currentPageSelectionManager.getSelectionStart();
        int selectionEnd = this.currentPageSelectionManager.getSelectionEnd();
        IntArrayList lineStartsList = new IntArrayList();
        ArrayList<BookEditScreen.Line> lines = new ArrayList<>();
        ArrayList<CaxtonText> caxtonTexts = new ArrayList<>(); // ← ADDED
        MutableInt lineIndexBox = new MutableInt();
        MutableBoolean newline = new MutableBoolean();
        FcIndexConverter warts = new FcIndexConverter();

        cth.wrapLines(content, 114, Style.EMPTY, true, warts, (style, start, end, rtl) -> {
            int lineIndex = lineIndexBox.getAndIncrement();
            String line = content.substring(start, end);
            newline.setValue(line.endsWith("\n"));
            String strippedLine = StringUtils.stripEnd(line, " \n");
            int y = lineIndex * this.textRenderer.fontHeight;
            BookEditScreen.Position screenPosition = this.absolutePositionToScreenPosition(BookEditScreenPositionAccessor.callInit(0, y));
            lineStartsList.add(start);
            BookEditScreen.Line bookLine = new BookEditScreen.Line(style, strippedLine, screenPosition.x, screenPosition.y);
            lines.add(bookLine);
            caxtonTexts.add(CaxtonText.fromFormatted(strippedLine, ctr.getFontStorageAccessor(), style, Style.EMPTY, false, rtl, cth.getCache())); // ← ADDED
        });
//        System.err.println(warts);
//        System.err.println(caxtonTexts);
        int[] lineStarts = lineStartsList.toIntArray();
        boolean cursorAtEnd = selectionStart == content.length();
        BookEditScreen.Position cursorPosition;
        if (cursorAtEnd && newline.isTrue()) {
            cursorPosition = BookEditScreenPositionAccessor.callInit(0, lines.size() * this.textRenderer.fontHeight);
        } else {
            int cursorLine = getLineFromOffset(lineStarts, selectionStart);
            /*
            int cursorX = this.textRenderer.getWidth(content.substring(lineStarts[cursorLine], selectionStart));
             */
            // Unfortunately, DrawableHelper.fill doesn’t have any overloads for float coordinates.
            int a = warts.formatfulToFormatless(lineStarts[cursorLine], true);
            int b = warts.formatfulToFormatless(selectionStart);
            warts.reset();
            int cursorX = Math.round(cth.getOffsetAtIndex(caxtonTexts.get(cursorLine), b - a, DirectionSetting.AUTO));
            cursorPosition = BookEditScreenPositionAccessor.callInit(cursorX, cursorLine * this.textRenderer.fontHeight);
        }
        ArrayList<Rect2i> selectionRects = Lists.newArrayList();
        if (selectionStart != selectionEnd) {
            int selectionLower = Math.min(selectionStart, selectionEnd);
            int selectionUpper = Math.max(selectionStart, selectionEnd);
            int lowerLineIndex = getLineFromOffset(lineStarts, selectionLower);
            int upperLineIndex = getLineFromOffset(lineStarts, selectionUpper);
            if (lowerLineIndex == upperLineIndex) {
                // Selection spans 1 line
                int y = lowerLineIndex * this.textRenderer.fontHeight;
                int lineStart = warts.formatfulToFormatless(lineStarts[lowerLineIndex], true);
                /*
                selectionRects.add(this.getLineSelectionRectangle(content, textHandler, selectionLower, selectionUpper, y, lineStart));
                */
                cth.getHighlightRanges(
                        caxtonTexts.get(lowerLineIndex),
                        warts.formatfulToFormatless(selectionLower) - lineStart,
                        warts.formatfulToFormatless(selectionUpper) - lineStart,
                        (left, right) -> selectionRects.add(
                                this.cxGetLineSelectionRectangle(left, right, y)));
            } else {
                // Draw highlight for first line
                int lineEnd = lowerLineIndex + 1 > lineStarts.length ? content.length() : lineStarts[lowerLineIndex + 1];
                /*
                selectionRects.add(this.getLineSelectionRectangle(content, textHandler, selectionLower, lineEnd, lowerLineIndex * this.textRenderer.fontHeight, lineStarts[lowerLineIndex]));
                 */
                int lineStart1 = warts.formatfulToFormatless(lineStarts[lowerLineIndex], true);
                cth.getHighlightRanges(
                        caxtonTexts.get(lowerLineIndex),
                        warts.formatfulToFormatless(selectionLower) - lineStart1,
                        warts.formatfulToFormatless(lineEnd) - lineStart1,
                        (left, right) -> selectionRects.add(
                                this.cxGetLineSelectionRectangle(left, right, lowerLineIndex * this.textRenderer.fontHeight)));

                // Draw highlight for middle lines
                for (int line = lowerLineIndex + 1; line < upperLineIndex; ++line) {
                    int y = line * this.textRenderer.fontHeight;
                    /*
                    String lineContents = content.substring(lineStarts[line], lineStarts[line + 1]);
                    int width = (int) textHandler.getWidth(lineContents);
                    selectionRects.add(this.getRectFromCorners(BookEditScreenPositionAccessor.callInit(0, y), BookEditScreenPositionAccessor.callInit(width, y + this.textRenderer.fontHeight)));
                    */
                    float width = cth.getWidth(caxtonTexts.get(line));
                    selectionRects.add(this.cxGetLineSelectionRectangle(0, width, y));
                }

                // Draw highlight for last line
                /*
                selectionRects.add(this.getLineSelectionRectangle(content, textHandler, lineStarts[upperLineIndex], selectionUpper, upperLineIndex * this.textRenderer.fontHeight, lineStarts[upperLineIndex]));
                */
                int a = warts.formatfulToFormatless(lineStarts[upperLineIndex], true);
                int b = warts.formatfulToFormatless(selectionUpper);
                cth.getHighlightRanges(
                        caxtonTexts.get(upperLineIndex),
                        0,
                        b - a,
                        (left, right) -> selectionRects.add(
                                this.cxGetLineSelectionRectangle(left, right, upperLineIndex * this.textRenderer.fontHeight)));
            }
            warts.reset();
        }
        /*
        return new BookEditScreen.PageContent(content, cursorPosition, cursorAtEnd, lineStarts, lines.toArray(new BookEditScreen.Line[0]), selectionRects.toArray(new Rect2i[0]));
         */
        BookEditScreen.PageContent pageContent = new BookEditScreen.PageContent(content, cursorPosition, cursorAtEnd, lineStarts, lines.toArray(new BookEditScreen.Line[0]), selectionRects.toArray(new Rect2i[0]));
        ((BookEditScreenPageContentExt) pageContent).setCaxtonText(caxtonTexts);
        ((BookEditScreenPageContentExt) pageContent).setWarts(warts);
        return pageContent;
    }

    private Rect2i cxGetLineSelectionRectangle(float left, float right, int lineY) {
        int x1 = Math.round(XSCALE * left), x2 = Math.round(XSCALE * right);
        return new Rect2i(x1, lineY + 32, x2 - x1, this.textRenderer.fontHeight);
    }

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/BufferBuilder;vertex(DDD)Lnet/minecraft/client/render/VertexConsumer;"), method = "drawSelection")
    private VertexConsumer onVertex(BufferBuilder bb, double x, double y, double z) {
        double scx = x / XSCALE + (width - 192) / 2.0 + 36;
        return bb.vertex(scx, y, z);
    }

    @ModifyExpressionValue(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/BookEditScreen;getPageContent()Lnet/minecraft/client/gui/screen/ingame/BookEditScreen$PageContent;"), method = "render")
    private BookEditScreen.PageContent drawPageContent(BookEditScreen.PageContent content, MatrixStack matrices, int mouseX, int mouseY, float delta) {
        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
        CaxtonTextRenderer ctr = ((HasCaxtonTextRenderer) textRenderer).getCaxtonTextRenderer();
        List<CaxtonText> cts = ((BookEditScreenPageContentExt) content).getCaxtonText();
        BookEditScreen.Line[] lines = ((BookEditScreenPageContentAccessor) content).getLines();
        for (int i = 0; i < lines.length; ++i) {
            BookEditScreen.Line line = lines[i];
            ctr.draw(
                    cts.get(i),
                    ((BookEditScreenLineAccessor) line).getX(),
                    ((BookEditScreenLineAccessor) line).getY(),
                    0xff000000, false,
                    matrices.peek().getPositionMatrix(), immediate,
                    false, 0, 0xF000F0,
                    -1, Float.POSITIVE_INFINITY);
        }
        immediate.draw();
        return content;
    }

    @Redirect(
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/TextRenderer;draw(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/text/Text;FFI)I"),
            slice = @Slice(
                    from = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/BookEditScreen;getPageContent()Lnet/minecraft/client/gui/screen/ingame/BookEditScreen$PageContent;")),
            method = "render")
    private int onDrawText(TextRenderer instance, MatrixStack matrices, Text text, float x, float y, int color) {
        return 0;
    }
}
