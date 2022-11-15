package xyz.flirora.caxton.render;

import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.GlyphRenderer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import org.joml.Matrix4f;
import xyz.flirora.caxton.font.*;
import xyz.flirora.caxton.mixin.TextRendererDrawerAccessor;

import java.util.List;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
public class CaxtonTextRenderer {
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

    private FontStorage getFontStorage(Identifier id) {
        return this.fontStorageAccessor.apply(id);
    }

    public float drawLayer(String text, float x, float y, int color, boolean shadow, Matrix4f matrix, VertexConsumerProvider vertexConsumerProvider, boolean seeThrough, int underlineColor, int light) {
        List<RunGroup> runGroups = Run.splitIntoGroups(text, fontStorageAccessor, false, this.rtl);
        float newX = drawRunGroups(x, y, color, shadow, matrix, vertexConsumerProvider, seeThrough, underlineColor, light, vanillaTextRenderer, runGroups);
        if (!shadow) this.rtl = false;
        return newX;
    }

    public float drawLayer(OrderedText text, float x, float y, int color, boolean shadow, Matrix4f matrix, VertexConsumerProvider vertexConsumerProvider, boolean seeThrough, int underlineColor, int light) {
        List<RunGroup> runGroups = Run.splitIntoGroups(text, fontStorageAccessor, false, this.rtl);
        return drawRunGroups(x, y, color, shadow, matrix, vertexConsumerProvider, seeThrough, underlineColor, light, vanillaTextRenderer, runGroups);
    }

    private float drawRunGroups(float x, float y, int color, boolean shadow, Matrix4f matrix, VertexConsumerProvider vertexConsumerProvider, boolean seeThrough, int underlineColor, int light, TextRenderer vanillaTextRenderer, List<RunGroup> runGroups) {
        float origX = x;
        TextRenderer.Drawer drawer = vanillaTextRenderer.new Drawer(vertexConsumerProvider, x, y, color, shadow, matrix, seeThrough, light);
        for (RunGroup runGroup : runGroups) {
            if (runGroup.getFont() == null) {
                for (Run run : runGroup.getStyleRuns()) {
                    ((TextRendererDrawerAccessor) drawer).setX(x);
                    run.text().codePoints().forEach(codePoint -> {
                        drawer.accept(0, run.style(), codePoint);
                    });
                }
                x = drawer.drawLayer(underlineColor, x);
            } else {
                ShapingResult[] shapingResults = runGroup.shape(this.handler.getShapingCache());

                for (int index = 0; index < shapingResults.length; ++index) {
                    ShapingResult shapingResult = shapingResults[index];
                    x = drawShapedRun(shapingResult, runGroup, index, x, y, color, shadow, matrix, vertexConsumerProvider, seeThrough, light, drawer);
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
            boolean seeThrough, int light,
            TextRenderer.Drawer drawer) {
        ConfiguredCaxtonFont configuredFont = runGroup.getFont();
        CaxtonFont font = configuredFont.font();
        CaxtonFontOptions options = font.getOptions();

        double shrink = options.shrinkage();
        int margin = options.margin();
        float shadowOffset = configuredFont.shadowOffset();
        float pageSize = (float) options.pageSize();

        int offset = runGroup.getBidiRuns()[3 * index];

        TextRenderer.TextLayerType layerType = seeThrough ? TextRenderer.TextLayerType.SEE_THROUGH : TextRenderer.TextLayerType.NORMAL;

        int ascender = font.getMetrics(CaxtonFont.Metrics.ASCENDER);
        int underlinePosition = font.getMetrics(CaxtonFont.Metrics.UNDERLINE_POSITION);
        int underlineThickness = font.getMetrics(CaxtonFont.Metrics.UNDERLINE_THICKNESS);
        int strikeoutPosition = font.getMetrics(CaxtonFont.Metrics.STRIKEOUT_POSITION);
        int strikeoutThickness = font.getMetrics(CaxtonFont.Metrics.STRIKEOUT_THICKNESS);

        float scale = 7.0f / ascender;
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
            if (atlasLoc == -1) {
                cumulAdvanceX += advanceX;
                continue;
            }

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

            RenderLayer renderLayer = CaxtonTextRenderLayers.text(atlasPage.getId(), seeThrough);
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

            float x0a = x + cumulAdvanceX * scale;
            float x1a = x + (cumulAdvanceX + advanceX) * scale;
            if (style.isUnderlined()) {
                ((TextRendererDrawerAccessor) drawer).callAddRectangle(new GlyphRenderer.Rectangle(x0a, y0u, x1a, y1u, 0.01f, red, green, blue, alpha));
            } else if (style.isStrikethrough()) {
                ((TextRendererDrawerAccessor) drawer).callAddRectangle(new GlyphRenderer.Rectangle(x0a, y0s, x1a, y1s, 0.01f, red, green, blue, alpha));
            }

            cumulAdvanceX += advanceX;
        }
        return x + cumulAdvanceX * scale;
    }

    public void clearCaches() {
        this.handler.clearCaches();
    }
}
