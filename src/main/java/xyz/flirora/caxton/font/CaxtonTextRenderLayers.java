package xyz.flirora.caxton.font;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.function.Function;

@Environment(EnvType.CLIENT)
public class CaxtonTextRenderLayers extends RenderLayer {
    private static final Function<Identifier, RenderLayer> TEXT = Util.memoize(
            texture -> RenderLayer.of(
                    "caxton_text",
                    VertexFormats.POSITION_COLOR_TEXTURE_LIGHT,
                    VertexFormat.DrawMode.QUADS,
                    256,
                    false,
                    true,
                    RenderLayer.MultiPhaseParameters.builder()
                            .shader(TEXT_SHADER)
                            .texture(new RenderPhase.Texture((Identifier) texture, false, false))
                            .transparency(TRANSLUCENT_TRANSPARENCY)
                            .lightmap(ENABLE_LIGHTMAP)
                            .build(false)));
    private static final Function<Identifier, RenderLayer> TEXT_SEE_THROUGH = Util.memoize(
            texture -> RenderLayer.of(
                    "caxton_text_see_through",
                    VertexFormats.POSITION_COLOR_TEXTURE_LIGHT,
                    VertexFormat.DrawMode.QUADS,
                    256,
                    false,
                    true,
                    RenderLayer.MultiPhaseParameters.builder()
                            .shader(TRANSPARENT_TEXT_SHADER)
                            .texture(new RenderPhase.Texture((Identifier) texture, false, false))
                            .transparency(TRANSLUCENT_TRANSPARENCY)
                            .lightmap(ENABLE_LIGHTMAP)
                            .build(false)));

    // not used; only here because I’m lazy
    public CaxtonTextRenderLayers(String name, VertexFormat vertexFormat, VertexFormat.DrawMode drawMode, int expectedBufferSize, boolean hasCrumbling, boolean translucent, Runnable startAction, Runnable endAction) {
        super(name, vertexFormat, drawMode, expectedBufferSize, hasCrumbling, translucent, startAction, endAction);
    }

    public static RenderLayer text(Identifier textureId, boolean seeThrough) {
        return (seeThrough ? TEXT_SEE_THROUGH : TEXT).apply(textureId);
    }
}
