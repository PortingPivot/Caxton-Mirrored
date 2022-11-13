package xyz.flirora.caxton.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import xyz.flirora.caxton.font.*;

import java.util.List;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
public class CaxtonTextRenderer {
    private final Function<Identifier, FontStorage> fontStorageAccessor;
    private final CaxtonTextHandler handler;
    private final TextRenderer vanillaTextRenderer;

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
        List<RunGroup> runGroups = Run.splitIntoGroups(text, fontStorageAccessor, false);
        return drawRunGroups(x, y, color, shadow, matrix, vertexConsumerProvider, seeThrough, underlineColor, light, vanillaTextRenderer, runGroups);
    }

    public float drawLayer(OrderedText text, float x, float y, int color, boolean shadow, Matrix4f matrix, VertexConsumerProvider vertexConsumerProvider, boolean seeThrough, int underlineColor, int light) {
        List<RunGroup> runGroups = Run.splitIntoGroups(text, fontStorageAccessor, false);
        return drawRunGroups(x, y, color, shadow, matrix, vertexConsumerProvider, seeThrough, underlineColor, light, vanillaTextRenderer, runGroups);
    }

    private float drawRunGroups(float x, float y, int color, boolean shadow, Matrix4f matrix, VertexConsumerProvider vertexConsumerProvider, boolean seeThrough, int underlineColor, int light, TextRenderer vanillaTextRenderer, List<RunGroup> runGroups) {
        for (RunGroup runGroup : runGroups) {
            if (runGroup.getFont() == null) {
                TextRenderer.Drawer drawer = vanillaTextRenderer.new Drawer(vertexConsumerProvider, x, y, color, shadow, matrix, seeThrough, light);
                for (Run run : runGroup.getStyleRuns()) {
                    run.text().codePoints().forEach(codePoint -> {
                        drawer.accept(0, run.style(), codePoint);
                    });
                }
                x = drawer.drawLayer(underlineColor, x);
            } else {
                ShapingResult[] shapingResults = runGroup.shape(this.handler.getShapingCache());

                for (int index = 0; index < shapingResults.length; ++index) {
                    ShapingResult shapingResult = shapingResults[index];
                    x = drawShapedRun(shapingResult, runGroup, index, x, y, color, shadow, matrix, vertexConsumerProvider, seeThrough, underlineColor, light);
                }
            }
        }
        return x;
    }

    private float drawShapedRun(ShapingResult shapedRun, RunGroup runGroup, int index, float x, float y, int color, boolean shadow, Matrix4f matrix, VertexConsumerProvider vertexConsumers, boolean seeThrough, int underlineColor, int light) {
        System.err.println(runGroup + " @ " + index);
        System.err.println(shapedRun);
        CaxtonFont font = runGroup.getFont();
        CaxtonFontOptions options = font.getOptions();

        double shrink = options.shrinkage();
        int margin = options.margin();
        float shadowOffset = options.shadowOffset();

        int offset = runGroup.getBidiRuns()[2 * index];

        TextRenderer.TextLayerType layerType = seeThrough ? TextRenderer.TextLayerType.SEE_THROUGH : TextRenderer.TextLayerType.NORMAL;
        float scale = 7.0f / font.getMetrics(CaxtonFont.Metrics.ASCENDER);

        float baselineY = y + 7.0f;

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
            float y1 = (float) (baselineY + (-offsetY - shrink * margin) * scale);
            float u0 = atlasX / 4096.0f;
            float v0 = atlasY / 4096.0f;
            float x1 = (float) (x + (gx + shrink * (atlasWidth - margin)) * scale);
            float y0 = (float) (baselineY + (-offsetY - shrink * (atlasHeight + margin)) * scale);
            float u1 = (atlasX + atlasWidth) / 4096.0f;
            float v1 = (atlasY + atlasHeight) / 4096.0f;

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

            // TODO: add underline and strikethrough effects if requested

            cumulAdvanceX += advanceX;
        }
        return x + cumulAdvanceX * scale;
    }

    public void clearCaches() {
        this.handler.clearCaches();
    }
}
