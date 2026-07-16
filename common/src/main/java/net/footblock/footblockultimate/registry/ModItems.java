package net.footblock.footblockultimate.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.footblock.footblockultimate.FootblockUltimate;
import net.footblock.footblockultimate.entity.FootballVariant;
import net.footblock.footblockultimate.item.FootballItem;
import net.footblock.footblockultimate.item.RefereeWhistleItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(FootblockUltimate.MOD_ID, Registries.ITEM);

    public static final RegistrySupplier<Item> FOOTBALL = ITEMS.register("football", () -> 
        new FootballItem(FootballVariant.CLASSIC, new Item.Properties().stacksTo(1))
    );

    public static final RegistrySupplier<Item> WORLD_CUP_2026_BALL = ITEMS.register("world_cup_2026_ball", () ->
        new FootballItem(FootballVariant.WORLD_CUP_2026, new Item.Properties().stacksTo(1))
    );

    public static final RegistrySupplier<Item> REFEREE_WHISTLE = ITEMS.register("referee_whistle", () ->
        new RefereeWhistleItem(new Item.Properties().stacksTo(1))
    );

    public static final RegistrySupplier<Item> RED_GOAL_LINE = ITEMS.register("red_goal_line", () ->
        new BlockItem(ModBlocks.RED_GOAL_LINE.get(), new Item.Properties())
    );

    public static final RegistrySupplier<Item> BLUE_GOAL_LINE = ITEMS.register("blue_goal_line", () ->
        new BlockItem(ModBlocks.BLUE_GOAL_LINE.get(), new Item.Properties())
    );

    public static final RegistrySupplier<Item> WORLD_CUP_TROPHY = ITEMS.register("world_cup_trophy", () ->
        new BlockItem(ModBlocks.WORLD_CUP_TROPHY.get(), new Item.Properties().stacksTo(1))
    );

    public static final RegistrySupplier<Item> SCORE_MANAGER_CONSOLE = ITEMS.register("score_manager_console", () ->
        new BlockItem(ModBlocks.SCORE_MANAGER_CONSOLE.get(), new Item.Properties().stacksTo(1))
    );

    private ModItems() {
    }

    public static void init() {
        ITEMS.register();
    }
}
