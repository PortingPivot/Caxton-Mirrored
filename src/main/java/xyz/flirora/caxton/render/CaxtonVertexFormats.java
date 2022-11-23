package xyz.flirora.caxton.render;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormatElement;
import net.minecraft.client.render.VertexFormats;

public class CaxtonVertexFormats {
    public static final VertexFormat POSITION_COLOR_COLOR_TEXTURE_LIGHT = new VertexFormat(
            ImmutableMap.<String, VertexFormatElement>builder()
                    .put("Position", VertexFormats.POSITION_ELEMENT)
                    .put("Color0", VertexFormats.COLOR_ELEMENT)
                    .put("Color1", VertexFormats.COLOR_ELEMENT)
                    .put("UV0", VertexFormats.TEXTURE_ELEMENT)
                    .put("UV2", VertexFormats.LIGHT_ELEMENT)
                    .build());
}
