package xyz.flirora.caxton.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.TextReorderingProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Environment(EnvType.CLIENT)
@Mixin(TranslationStorage.class)
public class TranslationStorageMixin {
    // Nip the fuckage in the bud
    @Inject(at = @At("HEAD"), method = "reorder", cancellable = true)
    private void onReorder(StringVisitable text, CallbackInfoReturnable<OrderedText> cir) {
        TextReorderingProcessor processor = TextReorderingProcessor.create(text);
        List<OrderedText> orderedTexts = processor.process(0, processor.getString().length(), false);
        cir.setReturnValue(OrderedText.concat(orderedTexts));
    }
}
