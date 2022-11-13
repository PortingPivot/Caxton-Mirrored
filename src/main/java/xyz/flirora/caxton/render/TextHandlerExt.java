package xyz.flirora.caxton.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface TextHandlerExt {
    void setCaxtonTextHandler(CaxtonTextHandler handler);
}
