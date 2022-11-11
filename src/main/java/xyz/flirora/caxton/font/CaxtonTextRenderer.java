package xyz.flirora.caxton.font;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.text.OrderedText;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

import java.util.*;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
public class CaxtonTextRenderer {
    private static final int MARGIN = 4; // See font.rs

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
        float totalWidth = 0;
        for (RunGroup runGroup : runGroups) {
            if (runGroup.getFont() == null) {
                TextRenderer.Drawer drawer = vanillaTextRenderer.new Drawer(vertexConsumerProvider, x + totalWidth, y, color, shadow, matrix, seeThrough, light);
                for (Run run : runGroup.getRuns()) {
                    run.text().codePoints().forEach(codePoint -> {
                        drawer.accept(0, run.style(), codePoint);
                    });
                }
                totalWidth += drawer.drawLayer(underlineColor, x);
            } else {
                ShapingResult[] shapingResults = shapeRunGroup(runGroup);

                System.out.println(Arrays.toString(shapingResults));
                for (ShapingResult shapingResult : shapingResults) {
                    totalWidth += drawShapedRun(shapingResult, runGroup.getFont(), x + totalWidth, y, color, shadow, matrix, vertexConsumerProvider, seeThrough, underlineColor, light);
                }
            }
        }
        return totalWidth;
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

        int numGlyphs = shapedRun.numGlyphs();
        int cumulAdvanceX = 0, cumulAdvanceY = 0;
        for (int i = 0; i < numGlyphs; ++i) {
            int glyphId = shapedRun.glyphId(i);
            int clusterIndex = shapedRun.clusterIndex(i);

            long glyphBbox = font.getBbox(glyphId);
            short bbXMin = (short) glyphBbox;
            short bbYMin = (short) (glyphBbox >> 16);
            short bbXMax = (short) (glyphBbox >> 32);
            short bbYMax = (short) (glyphBbox >> 48);
            int bbWidth = ((int) bbXMax) - ((int) bbXMin);
            int bbHeight = ((int) bbYMax) - ((int) bbYMin);

            long atlasLoc = font.getAtlasLocation(glyphId);
            int atlasX = (int) (atlasLoc & 0x1FF);
            int atlasY = (int) ((atlasLoc >> 13) & 0x1FF);
            int atlasWidth = (int) ((atlasLoc >> 26) & 0x1FF);
            int atlasHeight = (int) ((atlasLoc >> 39) & 0x1FF);
            int atlasPage = (int) (atlasLoc >>> 52);

            int advanceX = shapedRun.advanceX(i);
            int advanceY = shapedRun.advanceY(i);
            int offsetX = shapedRun.offsetX(i);
            int offsetY = shapedRun.offsetY(i);
            int ux = cumulAdvanceX + offsetX;
            int uy = cumulAdvanceY + offsetY;

            // ...

            cumulAdvanceX += advanceX;
            cumulAdvanceY += advanceY;
        }
        return 69.0f;
    }

    public void clearCaches() {
        shapingCache.clear();
    }

//    private RenderLayer getLayer(TextRenderer.TextLayerType layerType) {
//        return switch (layerType) {
//            default -> throw new IncompatibleClassChangeError();
//            case TextRenderer.TextLayerType.NORMAL -> this.textLayer;
//            case TextRenderer.TextLayerType.SEE_THROUGH -> this.seeThroughTextLayer;
//            case TextRenderer.TextLayerType.POLYGON_OFFSET -> this.polygonOffsetTextLayer;
//        };
//    }
}
