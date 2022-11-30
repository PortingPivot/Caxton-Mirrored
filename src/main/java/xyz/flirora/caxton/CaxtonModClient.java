package xyz.flirora.caxton;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import xyz.flirora.caxton.dll.LibraryLoading;

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

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing mod...");

        LibraryLoading.loadNativeLibrary(LOGGER);

        LOGGER.info("Registering built-in resource packs");

        ModContainer modContainer = FabricLoader.getInstance().getModContainer(MOD_ID).get();

        ResourceManagerHelper.registerBuiltinResourcePack(new Identifier(MOD_ID, "inter"), modContainer, Text.translatable("caxton.resourcePack.inter"), ResourcePackActivationType.NORMAL);
        ResourceManagerHelper.registerBuiltinResourcePack(new Identifier(MOD_ID, "opensans"), modContainer, Text.translatable("caxton.resourcePack.opensans"), ResourcePackActivationType.NORMAL);
    }
}
