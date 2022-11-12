package xyz.flirora.caxton.font;

import com.google.gson.JsonObject;
import net.minecraft.util.JsonHelper;

public record CaxtonFontOptions(double shrinkage, int margin, int range, boolean invert) {
    public CaxtonFontOptions(JsonObject json) {
        this(
                JsonHelper.getDouble(json, "shrinkage", 64.0),
                JsonHelper.getInt(json, "margin", 4),
                JsonHelper.getInt(json, "range", 2),
                JsonHelper.getBoolean(json, "invert", true)
        );
    }
}
