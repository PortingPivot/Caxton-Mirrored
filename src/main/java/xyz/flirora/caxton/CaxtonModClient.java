package xyz.flirora.caxton;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

@Environment(EnvType.CLIENT)
public class CaxtonModClient implements ClientModInitializer {
    public static final String MOD_ID = "caxton";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean FATAL_ON_BROKEN_METHOD_CALL = false;

    // This is meant to be called from a mixin method injecting into a Minecraft method.
    public static void onBrokenMethod() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        LOGGER.warn("Use of {}.{} detected.", stackTrace[3].getClassName(), stackTrace[3].getMethodName());
        for (int i = 3; i < stackTrace.length; ++i) {
            LOGGER.warn("    at " + stackTrace[i].toString());
        }
        LOGGER.warn("Do not use this method; its API is fundamentally broken.");
        if (FATAL_ON_BROKEN_METHOD_CALL) {
            throw new UnsupportedOperationException();
        }
    }

    private void loadNativeLibrary() {
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

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing mod...");

        loadNativeLibrary();

        LOGGER.info("Registering built-in resource packs");

        ModContainer modContainer = FabricLoader.getInstance().getModContainer(MOD_ID).get();

        ResourceManagerHelper.registerBuiltinResourcePack(new Identifier(MOD_ID, "inter"), modContainer, Text.translatable("caxton.resourcePack.inter"), ResourcePackActivationType.NORMAL);
        ResourceManagerHelper.registerBuiltinResourcePack(new Identifier(MOD_ID, "opensans"), modContainer, Text.translatable("caxton.resourcePack.opensans"), ResourcePackActivationType.NORMAL);
    }
}
