package xyz.flirora.caxton.layout;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Certain operations can take or return information corresponding to either the leftward edge of a glyph or the rightward edge. This enum dictates which edge should be considered.
 */
@Environment(EnvType.CLIENT)
public enum DirectionSetting {
    /**
     * Use the direction corresponding to that of the run in which the glyph resides.
     */
    AUTO,
    /**
     * Use the direction opposite of that of the run in which the glyph resides.
     */
    INVERT,
    /**
     * Always treat glyphs as if they were in left-to-right runs.
     */
    FORCE_LTR,
    /**
     * Always treat glyphs as if they were in right-to-left runs.
     */
    FORCE_RTL;

    /**
     * Given the actual direction of something, return whether it should be treated as right to left.
     *
     * @param actuallyRtl The actual direction of the thing, such as of a glyph.
     * @return Whether to treat the direction as being right to left.
     */
    public boolean treatAsRtl(boolean actuallyRtl) {
        return switch (this) {
            case AUTO -> actuallyRtl;
            case INVERT -> !actuallyRtl;
            case FORCE_LTR -> false;
            case FORCE_RTL -> true;
        };
    }
}
