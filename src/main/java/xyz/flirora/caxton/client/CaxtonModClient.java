package xyz.flirora.caxton.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import xyz.flirora.caxton.font.Run;

@Environment(EnvType.CLIENT)
public class CaxtonModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        System.out.println("initialize client");

        System.out.println("testing run lister...");
        System.out.println(Run.splitIntoGroups(
                Text.literal("hi hi ")
                        .append(Text.literal("hello ").formatted(Formatting.GOLD))
                        .append(Text.literal("1234").formatted(Formatting.AQUA, Formatting.BOLD))
                        .append(Text.literal("69").styled(style -> style.withFont(new Identifier("sexmod", "sexyfont"))))
                        .asOrderedText()));
    }
}
