package net.footblock.footblockultimate.registry;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.architectury.registry.registries.DeferredSupplier;
import net.footblock.footblockultimate.FootblockUltimate;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class ModCreativeTabs {
    private static final ResourceLocation TAB_ID = ResourceLocation.fromNamespaceAndPath(
            FootblockUltimate.MOD_ID,
            "footblock_ultimate"
    );
    private static final DeferredSupplier<CreativeModeTab> TAB_REFERENCE = CreativeTabRegistry.defer(TAB_ID);

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(FootblockUltimate.MOD_ID, Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<CreativeModeTab> FOOTBLOCK_ULTIMATE = TABS.register(
            "footblock_ultimate",
            () -> CreativeTabRegistry.create(builder -> builder
                    .title(Component.translatable("itemGroup.footblockultimate.main"))
                    .icon(() -> new ItemStack(ModItems.WORLD_CUP_2026_BALL.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.FOOTBALL.get());
                        output.accept(ModItems.WORLD_CUP_2026_BALL.get());
                        output.accept(ModItems.REFEREE_WHISTLE.get());
                        output.accept(ModItems.RED_GOAL_LINE.get());
                        output.accept(ModItems.BLUE_GOAL_LINE.get());
                        output.accept(ModItems.WORLD_CUP_TROPHY.get());
                        output.accept(ModItems.SCORE_MANAGER_CONSOLE.get());
                    }))
    );

    private ModCreativeTabs() {
    }

    public static void init() {
        TABS.register();
    }
}
