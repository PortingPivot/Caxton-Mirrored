package xyz.flirora.caxton.font;

import net.minecraft.text.CharacterVisitor;
import net.minecraft.text.Style;

/**
 * Like {@link CharacterVisitor}, but also accepts a direction flag.
 */
@FunctionalInterface
public interface DirectionalCharacterVisitor {
    static DirectionalCharacterVisitor fromCharacterVisitor(CharacterVisitor cv) {
        return (index, style, codePoint, rtl) -> cv.accept(index, style, codePoint);
    }

    boolean accept(int index, Style style, int codePoint, boolean rtl);
}
