package xyz.flirora.caxton;

import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class CaxtonMod implements ModInitializer {
    @Override
    public void onInitialize() {
        System.out.println("initialize");

        File runDirectory = MinecraftClient.getInstance().runDirectory;

        try (var libStream = getClass().getResourceAsStream("/linux-x86-64/libcaxtonbindings.so")) {
            if (libStream == null) {
                throw new FileNotFoundException("Could not find /linux-x86-64/libcaxtonbindings.so");
            }

            File tmp = new File(runDirectory, "libcaxtonbindings.so");
            try (var output = new FileOutputStream(tmp)) {
                libStream.transferTo(output);
            }

            System.load(tmp.toString());
            System.loadLibrary("harfbuzz");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
