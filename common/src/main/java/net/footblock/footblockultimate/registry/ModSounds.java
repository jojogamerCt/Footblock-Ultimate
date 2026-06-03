package net.footblock.footblockultimate.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.footblock.footblockultimate.FootblockUltimate;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(FootblockUltimate.MOD_ID, Registries.SOUND_EVENT);

    public static final RegistrySupplier<SoundEvent> FOOTBALL_KICK = SOUNDS.register("football_kick", () ->
            SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(FootblockUltimate.MOD_ID, "football_kick"))
    );

    public static void init() {
        SOUNDS.register();
    }
}
