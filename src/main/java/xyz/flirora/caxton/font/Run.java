package xyz.flirora.caxton.font;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.FontStorage;
import net.minecraft.text.*;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A run of characters with the same style.
 *
 * @param text  The characters in this run. These are always in logical order.
 * @param style The style shared by the characters.
 * @param font  The Caxton font used by this run, or null if this run is in a legacy font.
 */
@Environment(EnvType.CLIENT)
public record Run(String text, Style style, @Nullable ConfiguredCaxtonFont font) {
    @NotNull
    public static List<Run> splitIntoRuns(OrderedText text, Function<Identifier, FontStorage> fonts, boolean validateAdvance, boolean rtl) {
        RunLister lister = new RunLister(fonts, validateAdvance, rtl);
        text.accept(lister);
        return lister.getRuns();
    }

    @NotNull
    public static List<Run> splitIntoRunsFormatted(StringVisitable text, Function<Identifier, FontStorage> fonts, Style style, boolean validateAdvance, boolean rtl) {
        RunLister lister = new RunLister(fonts, validateAdvance, rtl);
        TextVisitFactory.visitFormatted(text, style, lister);
        return lister.getRuns();
    }

    @NotNull
    public static List<Run> splitIntoRunsFormatted(String text, Function<Identifier, FontStorage> fonts, Style style, boolean validateAdvance, boolean rtl) {
        RunLister lister = new RunLister(fonts, validateAdvance, rtl);
        TextVisitFactory.visitFormatted(text, style, lister);
        return lister.getRuns();
    }

    @NotNull
    public static List<Run> splitIntoRunsForwards(String text, Function<Identifier, FontStorage> fonts, Style style, boolean validateAdvance, boolean rtl) {
        RunLister lister = new RunLister(fonts, validateAdvance, rtl);
        TextVisitFactory.visitForwards(text, style, lister);
        return lister.getRuns();
    }

    private static class PendingRun {
        private final StringBuffer contents;
        private final Style style;
        private final @Nullable ConfiguredCaxtonFont font;

        private PendingRun(StringBuffer contents, Style style, @Nullable ConfiguredCaxtonFont font) {
            this.contents = contents;
            this.style = style;
            this.font = font;
        }

        public void appendCodePoint(int codePoint) {
            contents.appendCodePoint(codePoint);
        }

        public Run bake(boolean rtl) {
            String text = contents.toString();

            return new Run(text, style, font);
        }
    }

    private static class RunLister implements CharacterVisitor {
        private final List<PendingRun> runs;
        private final Function<Identifier, FontStorage> fonts;
        private final boolean validateAdvance;
        private final boolean rtl;

        private RunLister(Function<Identifier, FontStorage> fonts, boolean validateAdvance, boolean rtl) {
            this.rtl = rtl;
            this.runs = new ArrayList<>();
            this.fonts = fonts;
            this.validateAdvance = validateAdvance;
        }

        @Override
        public boolean accept(int index, Style style, int codePoint) {
            ConfiguredCaxtonFont font = ((CaxtonFontStorage) fonts.apply(style.getFont())).getCaxtonGlyph(codePoint, validateAdvance, style).getCaxtonFont();
            if (runs.isEmpty()) {
                addNewRun(codePoint, style, font);
            } else {
                PendingRun lastRun = runs.get(runs.size() - 1);
                if (lastRun.style.equals(style) && lastRun.font == font) {
                    lastRun.appendCodePoint(codePoint);
                } else {
                    addNewRun(codePoint, style, font);
                }
            }
            return true;
        }

        private void addNewRun(int codePoint, Style style, @Nullable ConfiguredCaxtonFont font) {
            runs.add(new PendingRun(new StringBuffer().appendCodePoint(codePoint), style, font));
        }

        public List<Run> getRuns() {
            return runs.stream().map(pr -> pr.bake(rtl)).collect(Collectors.toList());
        }
    }
}
