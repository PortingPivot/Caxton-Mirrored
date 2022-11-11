package xyz.flirora.caxton.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Function;

@Environment(EnvType.CLIENT)
@Mixin(TextRenderer.class)
public interface TextRendererAccessor {
    @Accessor
    Function<Identifier, FontStorage> getFontStorageAccessor();
}
