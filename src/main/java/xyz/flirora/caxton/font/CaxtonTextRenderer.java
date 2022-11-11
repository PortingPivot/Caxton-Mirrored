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
        int totalWidth = 0;
        for (RunGroup runGroup : runGroups) {
            if (runGroup.getFont() == null) {
                TextRenderer.Drawer drawer = vanillaTextRenderer.new Drawer(vertexConsumerProvider, x, y, color, shadow, matrix, seeThrough, light);
                for (Run run : runGroup.getRuns()) {
                    run.text().codePoints().forEach(codePoint -> {
                        drawer.accept(0, run.style(), codePoint);
                    });
                }
                totalWidth += drawer.drawLayer(underlineColor, x);
            } else {
                ShapingResult[] shapingResults = shapeRunGroup(runGroup);

                System.out.println(Arrays.toString(shapingResults));
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

    // TODO: call this whenever fonts are reloaded
    public void clearCaches() {
        shapingCache.clear();
    }
}
