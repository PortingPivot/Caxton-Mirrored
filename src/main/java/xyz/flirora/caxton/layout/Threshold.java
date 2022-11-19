package xyz.flirora.caxton.layout;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class Threshold {
    private int threshold;

    public Threshold(int threshold) {
        this.threshold = threshold;
    }

    public int getValue() {
        return threshold;
    }

    public boolean shouldSkip(RunGroup runGroup) {
        return threshold >= 0 && !runGroup.containsIndex(threshold);
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
        int r0 = runGroup.getCharOffset() + start + shapingResult.clusterIndex(sri);
        int r1 = runGroup.getCharOffset() + start + shapingResult.clusterLimit(sri);
        if (threshold >= 0 && !(r0 <= threshold && threshold < r1)) {
            return true;
        }
        threshold = -1;
        return false;
    }
}
