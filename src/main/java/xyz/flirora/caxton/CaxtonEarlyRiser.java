package xyz.flirora.caxton;

import com.chocohead.mm.api.ClassTinkerers;
import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import net.minecraft.client.font.FontLoader;
import xyz.flirora.caxton.font.CaxtonFontLoader;

import java.util.function.Function;

/**
 * An early riser for the Caxton mod.
 * <p>
 * This has the primary purpose of adding a variant to the {@link net.minecraft.client.font.FontType} enum. It also initializes MixinExtras.
 */
public class CaxtonEarlyRiser implements Runnable {
    @Override
    public void run() {
        MappingResolver remapper = FabricLoader.getInstance().getMappingResolver();

        String fontType = remapper.mapClassName("intermediary", "net.minecraft.class_394");
        ClassTinkerers.enumBuilder(fontType, String.class, Function.class)
                .addEnum("CAXTON", "caxton", (Function<JsonObject, FontLoader>) CaxtonFontLoader::fromJson).build();

        MixinExtrasBootstrap.init();
    }
}
