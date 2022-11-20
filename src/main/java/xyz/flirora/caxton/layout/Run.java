package xyz.flirora.caxton.layout;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.FontStorage;
import net.minecraft.text.*;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.flirora.caxton.font.CaxtonFontStorage;
import xyz.flirora.caxton.font.ConfiguredCaxtonFont;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    public static List<Run> splitIntoRuns(OrderedText text, Function<Identifier, FontStorage> fonts, boolean validateAdvance) {
        RunLister lister = new RunLister(fonts, validateAdvance);
        text.accept(lister);
        return lister.getRuns();
    }

    @NotNull
    public static List<Run> splitIntoRunsFormatted(StringVisitable text, Function<Identifier, FontStorage> fonts, Style style, boolean validateAdvance) {
        RunLister lister = new RunLister(fonts, validateAdvance);
        TextVisitFactory.visitFormatted(text, style, lister);
        return lister.getRuns();
    }

    @NotNull
    public static List<Run> splitIntoRunsFormatted(String text, Function<Identifier, FontStorage> fonts, Style style, boolean validateAdvance) {
        RunLister lister = new RunLister(fonts, validateAdvance);
        TextVisitFactory.visitFormatted(text, style, lister);
        return lister.getRuns();
    }

    @NotNull
    public static List<Run> splitIntoRunsForwards(StringVisitable text, Function<Identifier, FontStorage> fonts, Style style, boolean validateAdvance) {
        RunLister lister = new RunLister(fonts, validateAdvance);
        text.visit((style1, asString) -> {
            TextVisitFactory.visitForwards(asString, style1.withParent(style), lister);
            return Optional.empty();
        }, style);
        return lister.getRuns();
    }

    @NotNull
    public static List<Run> splitIntoRunsForwards(String text, Function<Identifier, FontStorage> fonts, Style style, boolean validateAdvance) {
        RunLister lister = new RunLister(fonts, validateAdvance);
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

        public Run bake() {
            String text = contents.toString();

            return new Run(text, style, font);
        }
    }

    public static class RunLister implements CharacterVisitor {
        private final List<PendingRun> runs;
        private final Function<Identifier, FontStorage> fonts;
        private final boolean validateAdvance;

        public RunLister(Function<Identifier, FontStorage> fonts, boolean validateAdvance) {
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
            return runs.stream().map(PendingRun::bake).collect(Collectors.toList());
        }
    }
}
