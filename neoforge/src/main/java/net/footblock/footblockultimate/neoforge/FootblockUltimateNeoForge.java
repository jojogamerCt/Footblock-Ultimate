package net.footblock.footblockultimate.neoforge;

import net.footblock.footblockultimate.FootblockUltimate;
import net.footblock.footblockultimate.client.FootblockUltimateClient;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(FootblockUltimate.MOD_ID)
public final class FootblockUltimateNeoForge {
    public FootblockUltimateNeoForge() {
        // Run our common setup.
        FootblockUltimate.init();

        if (FMLEnvironment.dist.isClient()) {
            FootblockUltimateClient.init();
        }
    }
}
