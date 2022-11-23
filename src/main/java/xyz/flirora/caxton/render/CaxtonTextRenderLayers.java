package xyz.flirora.caxton.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import xyz.flirora.caxton.font.CaxtonFont;
import xyz.flirora.caxton.font.CaxtonFontLoader;
import xyz.flirora.caxton.font.CaxtonFontOptions;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

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
                            .program(textShader(texture))
                            .texture(new RenderPhase.Texture((Identifier) texture, true, false))
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
                            .program(transparentTextShader(texture))
                            .texture(new RenderPhase.Texture((Identifier) texture, true, false))
                            .transparency(TRANSLUCENT_TRANSPARENCY)
                            .lightmap(ENABLE_LIGHTMAP)
                            .build(false)));

    // not used; only here because I’m lazy
    public CaxtonTextRenderLayers(String name, VertexFormat vertexFormat, VertexFormat.DrawMode drawMode, int expectedBufferSize, boolean hasCrumbling, boolean translucent, Runnable startAction, Runnable endAction) {
        super(name, vertexFormat, drawMode, expectedBufferSize, hasCrumbling, translucent, startAction, endAction);
    }

    private static ShaderProgram textShader(Identifier texId) {
        return new Shayder(
                () -> CaxtonShaders.caxtonTextShader,
                CaxtonTextRenderLayers.handleTextShader(texId));
    }

    private static ShaderProgram transparentTextShader(Identifier texId) {
        return new Shayder(
                () -> CaxtonShaders.caxtonTextSeeThroughShader,
                CaxtonTextRenderLayers.handleTextShader(texId));
    }

    private static Consumer<net.minecraft.client.gl.ShaderProgram> handleTextShader(Identifier texId) {
        Identifier fontId = texId.withPath(path -> path.substring(0, path.lastIndexOf('/')));
        return shader -> {
            if (shader == null) return;
            CaxtonFont font = CaxtonFontLoader.getFontById(fontId);
            if (font == null) return; // How did we get here?
            GlUniform unitRange = ((ShaderExt) shader).getUnitRange();
            if (unitRange != null) {
                CaxtonFontOptions options = font.getOptions();
                unitRange.set(((float) options.range()) / options.pageSize());
            }
        };
    }

    public static RenderLayer text(Identifier textureId, boolean seeThrough) {
        return (seeThrough ? TEXT_SEE_THROUGH : TEXT).apply(textureId);
    }

    public static class Shayder extends RenderPhase.ShaderProgram {
        public Shayder(
                Supplier<net.minecraft.client.gl.ShaderProgram> supplier,
                Consumer<net.minecraft.client.gl.ShaderProgram> callback) {
            super(() -> {
                net.minecraft.client.gl.ShaderProgram shader = supplier.get();
                callback.accept(shader);
                return shader;
            });
        }
    }
}
