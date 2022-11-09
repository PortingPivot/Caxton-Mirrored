package xyz.flirora.caxton;

import com.chocohead.mm.api.ClassTinkerers;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import net.minecraft.client.font.FontLoader;
import xyz.flirora.caxton.font.CaxtonFontLoader;

import java.util.function.Function;

public class CaxtonEarlyRiser implements Runnable {
    @Override
    public void run() {
        MappingResolver remapper = FabricLoader.getInstance().getMappingResolver();

        String fontType = remapper.mapClassName("intermediary", "net.minecraft.class_394");
        ClassTinkerers.enumBuilder(fontType, String.class, Function.class)
                .addEnum("CAXTON", "caxton", (Function<JsonObject, FontLoader>) CaxtonFontLoader::fromJson).build();
    }
}
