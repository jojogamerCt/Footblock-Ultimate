package net.footblock.footblockultimate.fabric.client;

import net.footblock.footblockultimate.client.FootblockUltimateClient;
import net.fabricmc.api.ClientModInitializer;

public final class FootblockUltimateFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FootblockUltimateClient.init();
    }
}
