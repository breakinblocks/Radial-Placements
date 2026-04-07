package com.breakinblocks.radial_placements;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

@Mod(Radial_placements.MODID)
public class Radial_placements {
    public static final String MODID = "radial_placements";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Radial_placements(IEventBus modEventBus, ModContainer modContainer) {
        ModStructurePlacements.PLACEMENT_TYPES.register(modEventBus);
        modEventBus.addListener(this::onCommonSetup);
        LOGGER.info("Radial Placements loaded");
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        LOGGER.debug("[RadialPlacements] Registered placement types:");
        BuiltInRegistries.STRUCTURE_PLACEMENT.entrySet().forEach(entry ->
                LOGGER.debug("[RadialPlacements]   {} -> {}", entry.getKey().location(), entry.getValue())
        );
    }
}
