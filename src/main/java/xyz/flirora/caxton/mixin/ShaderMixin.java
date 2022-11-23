package xyz.flirora.caxton.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.resource.ResourceFactory;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.flirora.caxton.render.ShaderExt;

@Environment(EnvType.CLIENT)
@Mixin(ShaderProgram.class)
public abstract class ShaderMixin implements ShaderExt {
    @Nullable
    private GlUniform unitRange;

    @Override
    public @Nullable GlUniform getUnitRange() {
        return unitRange;
    }

    @Shadow
    public abstract @Nullable GlUniform getUniform(String name);

    @Inject(at = @At("RETURN"), method = "<init>(Lnet/minecraft/resource/ResourceFactory;Ljava/lang/String;Lnet/minecraft/client/render/VertexFormat;)V")
    private void onInit(ResourceFactory factory, String name, VertexFormat format, CallbackInfo ci) {
        this.unitRange = this.getUniform("UnitRange");
    }
}
