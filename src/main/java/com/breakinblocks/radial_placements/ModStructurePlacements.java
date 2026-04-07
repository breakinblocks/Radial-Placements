package com.breakinblocks.radial_placements;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModStructurePlacements {
    public static final DeferredRegister<StructurePlacementType<?>> PLACEMENT_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, Radial_placements.MODID);

    public static final DeferredHolder<StructurePlacementType<?>, StructurePlacementType<EquidistantRingPlacement>> EQUIDISTANT_RING =
            PLACEMENT_TYPES.register("equidistant_ring", () -> () -> EquidistantRingPlacement.CODEC);
}
