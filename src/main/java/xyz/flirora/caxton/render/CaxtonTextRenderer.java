package xyz.flirora.caxton.render;

import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.Glyph;
import net.minecraft.client.font.GlyphRenderer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import xyz.flirora.caxton.font.CaxtonAtlasTexture;
import xyz.flirora.caxton.font.CaxtonFont;
import xyz.flirora.caxton.font.CaxtonFontOptions;
import xyz.flirora.caxton.font.ConfiguredCaxtonFont;
import xyz.flirora.caxton.layout.*;
import xyz.flirora.caxton.mixin.TextRendererDrawerAccessor;

import java.util.function.Function;

@Environment(EnvType.CLIENT)
public class CaxtonTextRenderer {
    // Copied from TextRenderer
    private static final Vector3f FORWARD_SHIFT = new Vector3f(0.0f, 0.0f, 0.03f);
    private final Function<Identifier, FontStorage> fontStorageAccessor;
    private final CaxtonTextHandler handler;
    private final TextRenderer vanillaTextRenderer;
    private final Random RANDOM = Random.createLocal();
    public boolean rtl;

    public CaxtonTextRenderer(Function<Identifier, FontStorage> fontStorageAccessor, TextRenderer vanillaTextRenderer) {
        this.fontStorageAccessor = fontStorageAccessor;
        this.handler = new CaxtonTextHandler(fontStorageAccessor, vanillaTextRenderer.getTextHandler());
        this.vanillaTextRenderer = vanillaTextRenderer;
    }

    public static CaxtonTextRenderer getInstance() {
        return ((HasCaxtonTextRenderer) MinecraftClient.getInstance().textRenderer).getCaxtonTextRenderer();
    }

    // Copy of TextRenderer.tweakTransparency
    private static int tweakTransparency(int argb) {
        if ((argb & 0xFC000000) == 0) {
            return argb | 0xFF000000;
        }
        return argb;
    }

    public Function<Identifier, FontStorage> getFontStorageAccessor() {
        return fontStorageAccessor;
    }

    private FontStorage getFontStorage(Identifier id) {
        return this.fontStorageAccessor.apply(id);
    }

    public float drawLayer(String text, float x, float y, int color, boolean shadow, Matrix4f matrix, VertexConsumerProvider vertexConsumerProvider, boolean seeThrough, int underlineColor, int light, int leftmostCodePoint, float maxWidth) {
        TextRenderer.TextLayerType layerType = seeThrough ? TextRenderer.TextLayerType.SEE_THROUGH : TextRenderer.TextLayerType.NORMAL;
        CaxtonText runGroups = CaxtonText.fromFormatted(text, fontStorageAccessor, Style.EMPTY, false, this.rtl, handler.getCache());
        float newX = drawRunGroups(x, y, color, shadow, matrix, vertexConsumerProvider, layerType, underlineColor, light, runGroups, leftmostCodePoint, maxWidth);
        if (!shadow) this.rtl = false;
        return newX;
    }

    public float drawLayer(OrderedText text, float x, float y, int color, boolean shadow, Matrix4f matrix, VertexConsumerProvider vertexConsumerProvider, boolean seeThrough, int underlineColor, int light, int leftmostCodePoint, float maxWidth) {
        TextRenderer.TextLayerType layerType = seeThrough ? TextRenderer.TextLayerType.SEE_THROUGH : TextRenderer.TextLayerType.NORMAL;
        CaxtonText runGroups = CaxtonText.from(text, fontStorageAccessor, false, this.rtl, handler.getCache());
        return drawRunGroups(x, y, color, shadow, matrix, vertexConsumerProvider, layerType, underlineColor, light, runGroups, leftmostCodePoint, maxWidth);
    }

    public void drawWithOutline(OrderedText text, float x, float y, int color, int outlineColor, Matrix4f matrix, VertexConsumerProvider vertexConsumers, int light) {
        Threshold NO_THRESHOLD = new Threshold(-1);

        CaxtonText runGroups = CaxtonText.from(text, fontStorageAccessor, false, this.rtl, handler.getCache());
        int effectiveOutlineColor = tweakTransparency(outlineColor);
        int effectiveColor = tweakTransparency(color);

        TextRenderer.Drawer outlineDrawer = vanillaTextRenderer.new Drawer(vertexConsumers, 0.0f, 0.0f, effectiveOutlineColor, false, matrix, TextRenderer.TextLayerType.NORMAL, light);
        TextRenderer.Drawer centralDrawer = vanillaTextRenderer.new Drawer(vertexConsumers, 0.0f, 0.0f, effectiveColor, false, matrix, TextRenderer.TextLayerType.POLYGON_OFFSET, light);

        float origX = x;
        for (RunGroup runGroup : runGroups.runGroups()) {
            ConfiguredCaxtonFont font = runGroup.getFont();
            if (font == null) {
                MutableFloat xBox = new MutableFloat();
                for (int dx = -1; dx <= 1; ++dx) {
                    for (int dy = -1; dy <= 1; ++dy) {
                        if (dx == 0 && dy == 0) continue;
                        xBox.setValue(x);
                        int dxf = dx, dyf = dy;
                        runGroup.accept((index, style, codePoint) -> {
                            int index2 = runGroup.getCharOffset() + index;

                            FontStorage fontStorage = this.getFontStorage(style.getFont());
                            Glyph glyph = fontStorage.getGlyph(codePoint, false);
                            float shadowOffset = glyph.getShadowOffset();

                            ((TextRendererDrawerAccessor) outlineDrawer).setX(xBox.floatValue() + dxf * shadowOffset);
                            ((TextRendererDrawerAccessor) outlineDrawer).setY(y + dyf * shadowOffset);
                            xBox.add(glyph.getAdvance(style.isBold()));
                            return outlineDrawer.accept(index2, style, codePoint);
                        });
                    }
                }
                x = xBox.floatValue();
            } else {
                ShapingResult[] shapingResults = runGroup.getShapingResults();
                float shadowOffset = font.shadowOffset();

                for (int dx = -1; dx <= 1; ++dx) {
                    for (int dy = -1; dy <= 1; ++dy) {
                        if (dx == 0 && dy == 0) continue;
                        for (int index = 0; index < shapingResults.length; ++index) {
                            ShapingResult shapingResult = shapingResults[index];
                            drawShapedRun(shapingResult, runGroup, index,
                                    x + dx * shadowOffset, y + dy * shadowOffset,
                                    effectiveOutlineColor, false,
                                    matrix, vertexConsumers, TextRenderer.TextLayerType.NORMAL, light, outlineDrawer,
                                    NO_THRESHOLD, Float.POSITIVE_INFINITY);
                        }
                    }
                }

                for (ShapingResult shapingResult : shapingResults) {
                    x += font.getScale() * shapingResult.totalWidth();
                }
            }
        }

        drawRunGroups(origX, y, effectiveColor, false, matrix, vertexConsumers, TextRenderer.TextLayerType.POLYGON_OFFSET, 0, light, runGroups, -1, Float.POSITIVE_INFINITY);
    }

    public float draw(CaxtonText text, float x, float y, int color, boolean shadow, Matrix4f matrix, VertexConsumerProvider vertexConsumerProvider, boolean seeThrough, int backgroundColor, int light, int leftmostCodePoint, float maxWidth) {
        TextRenderer.TextLayerType layerType = seeThrough ? TextRenderer.TextLayerType.SEE_THROUGH : TextRenderer.TextLayerType.NORMAL;
        color = tweakTransparency(color);
        Matrix4f matrix4f = new Matrix4f(matrix);
        if (shadow) {
            this.drawRunGroups(x, y, color, true, matrix, vertexConsumerProvider, layerType, backgroundColor, light, text, leftmostCodePoint, maxWidth);
            matrix4f.translate(FORWARD_SHIFT);
        }
        x = this.drawRunGroups(x, y, color, false, matrix4f, vertexConsumerProvider, layerType, backgroundColor, light, text, leftmostCodePoint, maxWidth);
        return (int) x + (shadow ? 1 : 0);
    }

    private float drawRunGroups(
            float x, float y,
            int color, boolean shadow,
            Matrix4f matrix, VertexConsumerProvider vertexConsumerProvider,
            TextRenderer.TextLayerType layerType, int underlineColor, int light,
            CaxtonText text, int leftmostCodePoint, float maxWidth) {
//        System.err.println(text);
        Threshold threshold = new Threshold(leftmostCodePoint);
        float origX = x;
        float maxX = x + maxWidth;
        TextRenderer.Drawer drawer = vanillaTextRenderer.new Drawer(vertexConsumerProvider, x, y, color, shadow, matrix, layerType, light);
        for (RunGroup runGroup : text.runGroups()) {
            if (threshold.shouldSkip(runGroup)) {
                continue;
            }
            if (x >= maxX) break;
            if (runGroup.getFont() == null) {
                ((TextRendererDrawerAccessor) drawer).setX(x);
                runGroup.accept((index, style, codePoint) -> {
                    int index2 = runGroup.getCharOffset() + index;
                    if (threshold.updateLegacy(index2)) {
                        return true;
                    }
                    if (((TextRendererDrawerAccessor) drawer).getX() >= maxX + handler.getWidth(codePoint, style))
                        return false;
                    return drawer.accept(index2, style, codePoint);
                });
                x = drawer.drawLayer(underlineColor, x);
            } else {
                ShapingResult[] shapingResults = runGroup.getShapingResults();

                for (int index = 0; index < shapingResults.length; ++index) {
                    ShapingResult shapingResult = shapingResults[index];
                    x = drawShapedRun(shapingResult, runGroup, index, x, y, color, shadow, matrix, vertexConsumerProvider, layerType, light, drawer, threshold, maxX);
                }
            }
        }
        drawer.drawLayer(underlineColor, origX);
        return x;
    }

    private float drawShapedRun(
            ShapingResult shapedRun,
            RunGroup runGroup,
            int index,
            float x, float y,
            int color, boolean shadow,
            Matrix4f matrix, VertexConsumerProvider vertexConsumers,
            TextRenderer.TextLayerType layerType, int light,
            TextRenderer.Drawer drawer,
            Threshold threshold, float maxX) {
        if (x >= maxX) return x;

        ConfiguredCaxtonFont configuredFont = runGroup.getFont();
        CaxtonFont font = configuredFont.font();
        CaxtonFontOptions options = font.getOptions();

        double shrink = options.shrinkage();
        int margin = options.margin();
        float shadowOffset = configuredFont.shadowOffset();
        float pageSize = (float) options.pageSize();

        int offset = runGroup.getBidiRuns()[3 * index];

        int underlinePosition = font.getMetrics(CaxtonFont.Metrics.UNDERLINE_POSITION);
        int underlineThickness = font.getMetrics(CaxtonFont.Metrics.UNDERLINE_THICKNESS);
        int strikeoutPosition = font.getMetrics(CaxtonFont.Metrics.STRIKEOUT_POSITION);
        int strikeoutThickness = font.getMetrics(CaxtonFont.Metrics.STRIKEOUT_THICKNESS);

        float scale = configuredFont.getScale();
        float baselineY = y + 7.0f;

        float y0u = baselineY - (underlinePosition - 0.5f * underlineThickness) * scale;
        float y1u = baselineY - (underlinePosition + 0.5f * underlineThickness) * scale;
        float y0s = baselineY - (strikeoutPosition - 0.5f * strikeoutThickness) * scale;
        float y1s = baselineY - (strikeoutPosition + 0.5f * strikeoutThickness) * scale;

        float brightnessMultiplier = shadow ? 0.25f : 1.0f;
        float baseBlue = (color & 0xFF) / 255.0f * brightnessMultiplier;
        float baseGreen = ((color >> 8) & 0xFF) / 255.0f * brightnessMultiplier;
        float baseRed = ((color >> 16) & 0xFF) / 255.0f * brightnessMultiplier;
        float alpha = ((color >> 24) & 0xFF) / 255.0f;

        int numGlyphs = shapedRun.numGlyphs();
        int cumulAdvanceX = 0;
        for (int i = 0; i < numGlyphs; ++i) {
            int glyphId = shapedRun.glyphId(i);
            int clusterIndex = shapedRun.clusterIndex(i);

            if (threshold.updateCaxton(runGroup, index, shapedRun, i)) {
                continue;
            }

            Style style = runGroup.getStyleAt(offset + clusterIndex);
            if (style.isObfuscated() && font.getAtlasLocation(glyphId) != -1) {
                long atlasLoc = font.getAtlasLocation(glyphId);
                int width = (int) ((atlasLoc >> 26) & 0x1FFF);
                IntList others = font.getGlyphsByWidth().get(width);
                glyphId = others.getInt(RANDOM.nextInt(others.size()));
            }

            var styleColorObj = style.getColor();
            float red = baseRed, green = baseGreen, blue = baseBlue;
            if (styleColorObj != null) {
                int styleColor = styleColorObj.getRgb();
                red = ((styleColor >> 16) & 0xFF) / 255.0f * brightnessMultiplier;
                green = ((styleColor >> 8) & 0xFF) / 255.0f * brightnessMultiplier;
                blue = (styleColor & 0xFF) / 255.0f * brightnessMultiplier;
            }

            int advanceX = shapedRun.advanceX(i);
            int offsetX = shapedRun.offsetX(i);
            int offsetY = shapedRun.offsetY(i);
            int gx = cumulAdvanceX + offsetX;

            long atlasLoc = font.getAtlasLocation(glyphId);
            if (atlasLoc != -1) {
                int atlasX = (int) (atlasLoc & 0x1FFF);
                int atlasY = (int) ((atlasLoc >> 13) & 0x1FFF);
                int atlasWidth = (int) ((atlasLoc >> 26) & 0x1FFF);
                int atlasHeight = (int) ((atlasLoc >> 39) & 0x1FFF);
                int atlasPageIndex = (int) (atlasLoc >>> 52);
                CaxtonAtlasTexture atlasPage = font.getAtlasPage(atlasPageIndex);

                long glyphBbox = font.getBbox(glyphId);
                short bbXMin = (short) glyphBbox;
                short bbYMin = (short) (glyphBbox >> 16);
                short bbXMax = (short) (glyphBbox >> 32);
                short bbYMax = (short) (glyphBbox >> 48);
                int bbWidth = ((int) bbXMax) - ((int) bbXMin);
                int bbHeight = ((int) bbYMax) - ((int) bbYMin);
                gx += bbXMin;
                offsetY += bbYMin;

                RenderLayer renderLayer = CaxtonTextRenderLayers.text(atlasPage.getId(), layerType);
                VertexConsumer vertexConsumer = vertexConsumers.getBuffer(renderLayer);

                // Draw the quad

                float x0 = (float) (x + (gx - shrink * margin) * scale);
                float y1 = (float) (baselineY + (-offsetY + shrink * margin) * scale);
                float u0 = atlasX / pageSize;
                float v0 = atlasY / pageSize;
                float x1 = (float) (x + (gx + shrink * (atlasWidth - margin)) * scale);
                float y0 = (float) (baselineY + (-offsetY - shrink * (atlasHeight - margin)) * scale);
                float u1 = (atlasX + atlasWidth) / pageSize;
                float v1 = (atlasY + atlasHeight) / pageSize;

                if (x1 >= maxX) break;

                if (shadow) {
                    x0 += shadowOffset;
                    x1 += shadowOffset;
                    y0 += shadowOffset;
                    y1 += shadowOffset;
                }

                vertexConsumer.vertex(matrix, x0, y0, 0.0f)
                        .color(red, green, blue, alpha)
                        .texture(u0, v0)
                        .light(light)
                        .next();
                vertexConsumer.vertex(matrix, x0, y1, 0.0f)
                        .color(red, green, blue, alpha)
                        .texture(u0, v1)
                        .light(light)
                        .next();
                vertexConsumer.vertex(matrix, x1, y1, 0.0f)
                        .color(red, green, blue, alpha)
                        .texture(u1, v1)
                        .light(light)
                        .next();
                vertexConsumer.vertex(matrix, x1, y0, 0.0f)
                        .color(red, green, blue, alpha)
                        .texture(u1, v0)
                        .light(light)
                        .next();
            }

            float x0a = x + cumulAdvanceX * scale;
            float x1a = x + (cumulAdvanceX + advanceX) * scale;
            if (style.isUnderlined()) {
                ((TextRendererDrawerAccessor) drawer).callAddRectangle(new GlyphRenderer.Rectangle(x0a, y0u, x1a, y1u, 0.01f, red, green, blue, alpha));
            }
            if (style.isStrikethrough()) {
                ((TextRendererDrawerAccessor) drawer).callAddRectangle(new GlyphRenderer.Rectangle(x0a, y0s, x1a, y1s, 0.01f, red, green, blue, alpha));
            }

            cumulAdvanceX += advanceX;
        }
        return x + cumulAdvanceX * scale;
    }

    public void clearCaches() {
        this.handler.clearCaches();
    }

    public CaxtonTextHandler getHandler() {
        return handler;
    }
}
