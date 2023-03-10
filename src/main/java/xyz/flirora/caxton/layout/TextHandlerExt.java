package xyz.flirora.caxton.layout;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface TextHandlerExt {
    CaxtonTextHandler getCaxtonTextHandler();
    
    void setCaxtonTextHandler(CaxtonTextHandler handler);
}
