package net.footblock.footblockultimate.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.footblock.footblockultimate.FootblockUltimate;
import net.footblock.footblockultimate.item.FootballItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(FootblockUltimate.MOD_ID, Registries.ITEM);

    public static final RegistrySupplier<Item> FOOTBALL = ITEMS.register("football", () -> 
        new FootballItem(new Item.Properties().stacksTo(1))
    );

    public static void init() {
        ITEMS.register();
    }
}
