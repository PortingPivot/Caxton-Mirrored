package xyz.flirora.caxton.render;

import net.minecraft.client.gl.GlUniform;
import org.jetbrains.annotations.Nullable;

public interface ShaderExt {
    @Nullable
    GlUniform getUnitRange();
}
