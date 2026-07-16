package net.footblock.footblockultimate.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.footblock.footblockultimate.FootblockUltimate;
import net.footblock.footblockultimate.block.GoalLineBlock;
import net.footblock.footblockultimate.block.ScoreManagerConsoleBlock;
import net.footblock.footblockultimate.block.WorldCupTrophyBlock;
import net.footblock.footblockultimate.match.WorldCupMatchManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(FootblockUltimate.MOD_ID, Registries.BLOCK);

    public static final RegistrySupplier<Block> RED_GOAL_LINE = BLOCKS.register("red_goal_line", () ->
            new GoalLineBlock(WorldCupMatchManager.Side.BLUE, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(0.8f)
                    .sound(SoundType.WOOL)
                    .noCollission()
                    .noOcclusion()
                    .lightLevel(state -> 3))
    );

    public static final RegistrySupplier<Block> BLUE_GOAL_LINE = BLOCKS.register("blue_goal_line", () ->
            new GoalLineBlock(WorldCupMatchManager.Side.RED, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(0.8f)
                    .sound(SoundType.WOOL)
                    .noCollission()
                    .noOcclusion()
                    .lightLevel(state -> 3))
    );

    public static final RegistrySupplier<Block> WORLD_CUP_TROPHY = BLOCKS.register("world_cup_trophy", () ->
            new WorldCupTrophyBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion())
    );

    public static final RegistrySupplier<Block> SCORE_MANAGER_CONSOLE = BLOCKS.register("score_manager_console", () ->
            new ScoreManagerConsoleBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(2.5f, 6.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .lightLevel(state -> 4))
    );

    private ModBlocks() {
    }

    public static void init() {
        BLOCKS.register();
    }
}
