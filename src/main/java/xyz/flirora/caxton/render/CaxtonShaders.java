package xyz.flirora.caxton.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Shader;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class CaxtonShaders {
    @Nullable
    public static Shader caxtonTextShader, caxtonTextSeeThroughShader;
}
