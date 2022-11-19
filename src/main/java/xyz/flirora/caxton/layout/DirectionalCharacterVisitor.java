package xyz.flirora.caxton.layout;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.CharacterVisitor;
import net.minecraft.text.Style;

/**
 * Like {@link CharacterVisitor}, but also accepts a direction flag.
 */
@FunctionalInterface
@Environment(EnvType.CLIENT)
public interface DirectionalCharacterVisitor {
    static DirectionalCharacterVisitor fromCharacterVisitor(CharacterVisitor cv) {
        return (index, style, codePoint, rtl) -> cv.accept(index, style, codePoint);
    }

    boolean accept(int index, Style style, int codePoint, boolean rtl);
}
