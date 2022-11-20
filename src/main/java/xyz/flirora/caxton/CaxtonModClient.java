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

@Environment(EnvType.CLIENT)
public class CaxtonModClient implements ClientModInitializer {
    public static final String MOD_ID = "caxton";
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing mod (client side)...");
        LOGGER.info("Registering built-in resource packs");

        ModContainer modContainer = FabricLoader.getInstance().getModContainer(MOD_ID).get();

        ResourceManagerHelper.registerBuiltinResourcePack(new Identifier(MOD_ID, "inter"), modContainer, Text.translatable("caxton.resourcePack.inter"), ResourcePackActivationType.NORMAL);
        ResourceManagerHelper.registerBuiltinResourcePack(new Identifier(MOD_ID, "opensans"), modContainer, Text.translatable("caxton.resourcePack.opensans"), ResourcePackActivationType.NORMAL);
    }
}
