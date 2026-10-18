package com.sm1les.slimmer;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SlimmerMod.MODID);

    public static final DeferredItem<DeferredSpawnEggItem> SLIMMER_SPAWN_EGG =
            ITEMS.register("slimmer_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.SLIMMER, 0x3FA83F, 0x0A2A0A, new Item.Properties()));
}
