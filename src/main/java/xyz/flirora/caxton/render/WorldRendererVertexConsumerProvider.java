package xyz.flirora.caxton.render;

import com.google.common.collect.ImmutableSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import xyz.flirora.caxton.mixin.RenderPhaseAccessor;

import java.util.*;

/**
 * A version of {@link VertexConsumerProvider.Immediate} that retains all
 * Caxton text layers.
 */
@Environment(EnvType.CLIENT)
public class WorldRendererVertexConsumerProvider extends VertexConsumerProvider.Immediate {
    private static final Set<String> CAXTON_TEXT_RENDER_LAYER_NAMES = ImmutableSet.of("caxton_text", "caxton_text_see_through");

    private static final Map<RenderLayer, BufferBuilder> caxtonTextLayerBuilders = new HashMap<>();

    public WorldRendererVertexConsumerProvider(BufferBuilder fallbackBuffer, Map<RenderLayer, BufferBuilder> layerBuffers) {
        super(fallbackBuffer, layerBuffers);
    }

    private static boolean isCaxtonTextLayer(RenderLayer layer) {
        return CAXTON_TEXT_RENDER_LAYER_NAMES.contains(((RenderPhaseAccessor) layer).getName());
    }

    // The following methods are copied from VertexConsumerProvider.Immediate
    // (with some renaming).
    @Override
    public VertexConsumer getBuffer(RenderLayer renderLayer) {
        Optional<RenderLayer> optional = renderLayer.asOptional();
        BufferBuilder bufferBuilder = this.getBufferInternal(renderLayer);
        if (!Objects.equals(this.currentLayer, optional) || !renderLayer.areVerticesNotShared()) {
            RenderLayer currentLayer;
            if (this.currentLayer.isPresent() && !isCaxtonTextLayer(currentLayer = this.currentLayer.get()) && !this.layerBuffers.containsKey(currentLayer)) {
                this.draw(currentLayer);
            }
            if (this.activeConsumers.add(bufferBuilder)) {
                bufferBuilder.begin(renderLayer.getDrawMode(), renderLayer.getVertexFormat());
            }
            this.currentLayer = optional;
        }
        return bufferBuilder;
    }

    private BufferBuilder getBufferInternal(RenderLayer layer) {
        if (isCaxtonTextLayer(layer)) {
            return caxtonTextLayerBuilders.computeIfAbsent(layer, l -> new BufferBuilder(256));
        }
        return this.layerBuffers.getOrDefault(layer, this.fallbackBuffer);
    }

    @Override
    public void drawCurrentLayer() {
        if (this.currentLayer.isPresent()) {
            RenderLayer renderLayer = this.currentLayer.get();
            if (!isCaxtonTextLayer(renderLayer) && !this.layerBuffers.containsKey(renderLayer)) {
                this.draw(renderLayer);
            }
            this.currentLayer = Optional.empty();
        }
    }

    @Override
    public void draw() {
        super.draw();
        this.drawCaxtonTextLayers();
    }

    public void drawCaxtonTextLayers() {
        for (RenderLayer renderLayer : caxtonTextLayerBuilders.keySet()) {
            this.draw(renderLayer);
        }
    }

    // This must be overridden because the implementation in
    // VertexConsumerProvider.Immediate calls its own getBufferInternal method,
    // which is not overridden by this class’s implementation.
    @Override
    public void draw(RenderLayer layer) {
        BufferBuilder bufferBuilder = this.getBufferInternal(layer);
        boolean layerIsCurrent = Objects.equals(this.currentLayer, layer.asOptional());
        if (!layerIsCurrent && bufferBuilder == this.fallbackBuffer) {
            return;
        }
        if (!this.activeConsumers.remove(bufferBuilder)) {
            return;
        }
        layer.draw(bufferBuilder, 0, 0, 0);
        if (layerIsCurrent) {
            this.currentLayer = Optional.empty();
        }
    }
}
