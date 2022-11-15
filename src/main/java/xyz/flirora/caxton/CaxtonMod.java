package xyz.flirora.caxton;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class CaxtonMod implements ModInitializer {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing mod...");
        LOGGER.info(
                "Extracting and loading library for {} / {}",
                System.getProperty("os.name"),
                System.getProperty("os.arch"));

        File runDirectory = MinecraftClient.getInstance().runDirectory;

        String soName = System.mapLibraryName("caxton_impl");

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
