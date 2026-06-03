package net.footblock.footblockultimate.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.footblock.footblockultimate.FootblockUltimate;
import net.footblock.footblockultimate.entity.FootballEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(FootblockUltimate.MOD_ID, Registries.ENTITY_TYPE);

    public static final RegistrySupplier<EntityType<FootballEntity>> FOOTBALL = ENTITIES.register("football", () ->
        EntityType.Builder.<FootballEntity>of(FootballEntity::new, MobCategory.MISC)
            .sized(0.5f, 0.5f)
            .clientTrackingRange(10)
            .updateInterval(1)
            .build("football")
    );

    public static void init() {
        ENTITIES.register();
    }
}
