package xyz.flirora.caxton.font;

import com.google.gson.JsonObject;
import net.minecraft.util.JsonHelper;

public record CaxtonFontOptions(
        double shrinkage,
        int margin,
        int range,
        boolean invert,
        int pageSize) {
    public CaxtonFontOptions {
        if (shrinkage <= 0.0) {
            throw new IllegalArgumentException("shrinkage must be positive");
        }
        if (margin < 0) {
            throw new IllegalArgumentException("margin cannot be negative");
        }
        if (range < 0) {
            throw new IllegalArgumentException("range cannot be negative");
        }
        if (pageSize < 64 || pageSize > 4096) {
            throw new IllegalArgumentException("page size must be in [64, 4096]");
        }
    }

    public CaxtonFontOptions(JsonObject json) {
        this(
                JsonHelper.getDouble(json, "shrinkage", 64.0),
                JsonHelper.getInt(json, "margin", 4),
                JsonHelper.getInt(json, "range", 2),
                JsonHelper.getBoolean(json, "invert", true),
                JsonHelper.getInt(json, "page_size", 4096)
        );
    }
}
