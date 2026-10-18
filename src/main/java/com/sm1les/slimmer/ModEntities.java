package com.sm1les.slimmer;

import com.sm1les.slimmer.entity.Slimmer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, SlimmerMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<Slimmer>> SLIMMER =
            ENTITIES.register("slimmer", () -> EntityType.Builder
                    .of(Slimmer::new, MobCategory.MONSTER)
                    .sized(0.7F, 1.6F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("slimmer"));
}
