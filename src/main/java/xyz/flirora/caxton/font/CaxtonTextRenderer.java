package xyz.flirora.caxton.font;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.text.OrderedText;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

import java.util.*;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
public class CaxtonTextRenderer {
    // TODO: make these configurable
    private static final int MARGIN = 4; // See font.rs
    private static final float SHRINK = 64.0f; // See font.rs

    private final Function<Identifier, FontStorage> fontStorageAccessor;

    private final Map<CaxtonFont, Map<String, ShapingResult>> shapingCache = new IdentityHashMap<>();

    public CaxtonTextRenderer(Function<Identifier, FontStorage> fontStorageAccessor) {
        this.fontStorageAccessor = fontStorageAccessor;
    }

    public static CaxtonTextRenderer getInstance() {
        return ((HasCaxtonTextRenderer) MinecraftClient.getInstance().textRenderer).getCaxtonTextRenderer();
    }

    private FontStorage getFontStorage(Identifier id) {
        return this.fontStorageAccessor.apply(id);
    }

    public float drawLayer(OrderedText text, float x, float y, int color, boolean shadow, Matrix4f matrix, VertexConsumerProvider vertexConsumerProvider, boolean seeThrough, int underlineColor, int light) {
        TextRenderer vanillaTextRenderer = MinecraftClient.getInstance().textRenderer;

        List<RunGroup> runGroups = Run.splitIntoGroups(text, fontStorageAccessor, false);
        for (RunGroup runGroup : runGroups) {
            if (runGroup.getFont() == null) {
                TextRenderer.Drawer drawer = vanillaTextRenderer.new Drawer(vertexConsumerProvider, x, y, color, shadow, matrix, seeThrough, light);
                for (Run run : runGroup.getRuns()) {
                    run.text().codePoints().forEach(codePoint -> {
                        drawer.accept(0, run.style(), codePoint);
                    });
                }
                x = drawer.drawLayer(underlineColor, x);
            } else {
                ShapingResult[] shapingResults = shapeRunGroup(runGroup);

                System.out.println(Arrays.toString(shapingResults));
                for (ShapingResult shapingResult : shapingResults) {
                    x = drawShapedRun(shapingResult, runGroup.getFont(), x, y, color, shadow, matrix, vertexConsumerProvider, seeThrough, underlineColor, light);
                }
            }
        }
        return x;
    }

    private ShapingResult[] shapeRunGroup(RunGroup runGroup) {
        CaxtonFont font = runGroup.getFont();

        if (font == null) {
            throw new UnsupportedOperationException("shapeRunGroup requires a Caxton font (got a legacy font)");
        }

        var shapingCache = this.shapingCache.computeIfAbsent(font, f -> new HashMap<>());

        // Determine which runs need to be shaped
        int[] bidiRuns = runGroup.getBidiRuns();
        IntList uncachedBidiRuns = new IntArrayList(bidiRuns.length / 2);
        ShapingResult[] shapingResults = new ShapingResult[bidiRuns.length / 2];
        for (int i = 0; i < bidiRuns.length / 2; ++i) {
            int start = bidiRuns[2 * i];
            int end = bidiRuns[2 * i + 1];
            String s = new String(runGroup.getJoined(), start, end - start);
            ShapingResult sr = shapingCache.get(s);
            if (sr != null) {
                shapingResults[i] = sr;
            } else {
                uncachedBidiRuns.add(start);
                uncachedBidiRuns.add(end);
            }
        }

        ShapingResult[] newlyComputed = font.shape(runGroup.getJoined(), uncachedBidiRuns.toIntArray());

        // Fill in blanks from before
        for (int i = 0, j = 0; i < bidiRuns.length / 2; ++i) {
            if (shapingResults[i] == null) {
                shapingResults[i] = newlyComputed[j];

                int start = bidiRuns[2 * i];
                int end = bidiRuns[2 * i + 1];
                String s = new String(runGroup.getJoined(), start, end - start);
                shapingCache.put(s, newlyComputed[j]);

                ++j;
            }
        }

        return shapingResults;
    }

    private float drawShapedRun(ShapingResult shapedRun, CaxtonFont font, float x, float y, int color, boolean shadow, Matrix4f matrix, VertexConsumerProvider vertexConsumers, boolean seeThrough, int underlineColor, int light) {
        TextRenderer.TextLayerType layerType = seeThrough ? TextRenderer.TextLayerType.SEE_THROUGH : TextRenderer.TextLayerType.NORMAL;
        float scale = 7.0f / font.getMetrics(CaxtonFont.Metrics.ASCENDER);

        float baselineY = y + 7.0f;

        // TODO: account for style
        float red = (color & 0xFF) / 255.0f;
        float green = ((color >> 8) & 0xFF) / 255.0f;
        float blue = ((color >> 16) & 0xFF) / 255.0f;
        float alpha = ((color >> 24) & 0xFF) / 255.0f;

        int numGlyphs = shapedRun.numGlyphs();
        int cumulAdvanceX = 0, cumulAdvanceY = 0;
        for (int i = 0; i < numGlyphs; ++i) {
            int glyphId = shapedRun.glyphId(i);
            // TODO: use this to compute the appropriate style for the glyph
            int clusterIndex = shapedRun.clusterIndex(i);

            int advanceX = shapedRun.advanceX(i);
            int advanceY = shapedRun.advanceY(i);
            int offsetX = shapedRun.offsetX(i);
            int offsetY = shapedRun.offsetY(i);
            int gx = cumulAdvanceX + offsetX;
            int gy = cumulAdvanceY + offsetY;

            long atlasLoc = font.getAtlasLocation(glyphId);
            if (atlasLoc == -1) {
                cumulAdvanceX += advanceX;
                cumulAdvanceY += advanceY;
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

            RenderLayer renderLayer = CaxtonTextRenderLayers.text(atlasPage.getId(), seeThrough);
            VertexConsumer vertexConsumer = vertexConsumers.getBuffer(renderLayer);

            // Draw the quad

            float x0 = x + (gx - SHRINK * MARGIN) * scale;
            float y1 = baselineY + (-gy - SHRINK * MARGIN) * scale;
            float u0 = atlasX / 4096.0f;
            float v0 = atlasY / 4096.0f;
            float x1 = x + (gx + SHRINK * (atlasWidth - MARGIN)) * scale;
            float y0 = baselineY + (-gy - SHRINK * (atlasHeight + MARGIN)) * scale;
            float u1 = (atlasX + atlasWidth) / 4096.0f;
            float v1 = (atlasY + atlasHeight) / 4096.0f;

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
            cumulAdvanceY += advanceY;
        }
        return x + cumulAdvanceX * scale;
    }

    public void clearCaches() {
        shapingCache.clear();
    }
}
