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

        String soName = "libcaxton_impl.so";

        try (var libStream = getClass().getResourceAsStream("/" + soName)) {
            if (libStream == null) {
                throw new FileNotFoundException("Could not find " + soName);
            }

            File tmp = new File(runDirectory, soName);
            try (var output = new FileOutputStream(tmp)) {
                libStream.transferTo(output);
            }

            System.load(tmp.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
