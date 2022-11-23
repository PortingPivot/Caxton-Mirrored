package xyz.flirora.caxton.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.GlyphRenderer;
import net.minecraft.client.font.TextRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Environment(EnvType.CLIENT)
@Mixin(TextRenderer.Drawer.class)
public interface TextRendererDrawerAccessor {
    // Used for rendering with a maximum width.
    @Accessor("x")
    float getX();

    // Not really necessary, but allows us to avoid redundant creation of
    // TextRenderer.Drawer objects.
    @Accessor("x")
    void setX(float x);

    @Accessor("y")
    float getY();

    @Accessor("y")
    void setY(float y);

    @Invoker
    void callAddRectangle(GlyphRenderer.Rectangle rectangle);
}
