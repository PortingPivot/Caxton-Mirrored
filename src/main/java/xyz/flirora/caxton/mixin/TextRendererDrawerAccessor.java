package xyz.flirora.caxton.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin(TextRenderer.Drawer.class)
public interface TextRendererDrawerAccessor {
    // Not really necessary, but allows us to avoid redundant creation of
    // TextRenderer.Drawer objects.
    @Accessor("x")
    void setX(float x);
}
