package xyz.flirora.caxton.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;

@Environment(EnvType.CLIENT)
public class CaxtonModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        System.out.println("initialize client");
    }
}
