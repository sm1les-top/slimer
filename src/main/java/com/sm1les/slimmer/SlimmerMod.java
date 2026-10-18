package com.sm1les.slimmer;

import com.sm1les.slimmer.entity.Slimmer;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

@Mod(SlimmerMod.MODID)
public class SlimmerMod {
    public static final String MODID = "slimmer";

    public SlimmerMod(IEventBus modBus) {
        ModEntities.ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
        modBus.addListener(this::onAttributes);
        modBus.addListener(this::onSpawnPlacements);
        modBus.addListener(this::onCreativeTab);
    }

    private void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.SLIMMER.get(), Slimmer.createAttributes().build());
    }

    private void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(
                ModEntities.SLIMMER.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.OR);
    }

    private void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.SLIMMER_SPAWN_EGG.get());
        }
    }
}
