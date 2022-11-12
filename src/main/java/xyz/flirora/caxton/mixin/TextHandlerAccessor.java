package xyz.flirora.caxton.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin(TextHandler.class)
public interface TextHandlerAccessor {
    @Accessor
    TextHandler.WidthRetriever getWidthRetriever();
}
