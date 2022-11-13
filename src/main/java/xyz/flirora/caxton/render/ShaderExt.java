package xyz.flirora.caxton.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.GlUniform;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public interface ShaderExt {
    @Nullable
    GlUniform getUnitRange();
}
