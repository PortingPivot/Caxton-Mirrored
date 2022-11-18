package xyz.flirora.caxton.render;

import xyz.flirora.caxton.font.RunGroup;
import xyz.flirora.caxton.font.ShapingResult;

public class Threshold {
    private int threshold;

    public Threshold(int threshold) {
        this.threshold = threshold;
    }

    public int getValue() {
        return threshold;
    }

    public boolean shouldSkip(RunGroup runGroup) {
        return threshold >= 0 && (threshold < runGroup.getCharOffset() || threshold >= runGroup.getCharOffset() + runGroup.getTotalLength());
    }

    public boolean updateLegacy(int index) {
        if (threshold >= 0 && threshold != index) {
            return true;
        }
        threshold = -1;
        return false;
    }

    public boolean updateCaxton(RunGroup runGroup, int bri, ShapingResult shapingResult, int sri) {
        int[] bidiRuns = runGroup.getBidiRuns();
        int start = bidiRuns[3 * bri];
        int end = bidiRuns[3 * bri + 1];
        int r0 = start + shapingResult.clusterIndex(sri);
        int prev = start + (sri == 0 ? 0 : shapingResult.clusterIndex(sri - 1));
        int next = sri == shapingResult.numGlyphs() - 1 ? end : start + shapingResult.clusterIndex(sri + 1);
        int r1 = Math.max(prev, next);
        r0 += runGroup.getCharOffset();
        r1 += runGroup.getCharOffset();
        if (threshold >= 0 && !(r0 <= threshold && threshold < r1)) {
            return true;
        }
        threshold = -1;
        return false;
    }
}
