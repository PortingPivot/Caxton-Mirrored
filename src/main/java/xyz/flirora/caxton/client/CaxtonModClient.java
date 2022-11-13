package xyz.flirora.caxton.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import xyz.flirora.caxton.font.Run;
import xyz.flirora.caxton.mixin.TextRendererAccessor;

@Environment(EnvType.CLIENT)
public class CaxtonModClient implements ClientModInitializer {
    public static final String MOD_ID = "caxton";

    public static void testRunLister(MinecraftClient client) {
        System.out.println("testing run lister...");

        var fontStorageAccessor = ((TextRendererAccessor) client.textRenderer).getFontStorageAccessor();

        Run.splitIntoGroups(
                        Text.literal("hi テスト hi ")
                                .append(Text.literal("hello ").formatted(Formatting.GOLD))
                                .append(Text.literal("1234").formatted(Formatting.AQUA, Formatting.BOLD))
                                .append(Text.literal("69").styled(style -> style.withFont(new Identifier("minecraft", "illageralt"))))
                                .append(Text.literal("架\uD83E\uDD22空\uD83E\uDD22言\uD83E\uDD22語").formatted(Formatting.DARK_GREEN, Formatting.ITALIC))
                                .asOrderedText(),
                        fontStorageAccessor,
                        false,
                        false)
                .forEach(System.out::println);
    }

    @Override
    public void onInitializeClient() {
        System.out.println("initialize client");

        ModContainer modContainer = FabricLoader.getInstance().getModContainer(MOD_ID).get();

        ResourceManagerHelper.registerBuiltinResourcePack(new Identifier(MOD_ID, "inter"), modContainer, ResourcePackActivationType.NORMAL);
        ResourceManagerHelper.registerBuiltinResourcePack(new Identifier(MOD_ID, "opensans"), modContainer, ResourcePackActivationType.NORMAL);
    }
}
