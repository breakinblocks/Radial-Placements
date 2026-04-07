package com.breakinblocks.radial_placements;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.stream.Stream;

public class EquidistantRingPlacement extends StructurePlacement {
    private static final Logger LOGGER = LogUtils.getLogger();
    private volatile boolean hasLoggedPositions = false;

    // Hand-written MapCodec for all ring parameters — bypasses RecordCodecBuilder/instance.group()
    // entirely to avoid DFU arity issues with 9 fields
    private static final MapCodec<RingConfig> RING_CONFIG_CODEC = new MapCodec<>() {
        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.of(
                    "label", "distance", "count", "spread_tolerance", "distance_variance",
                    "center_x", "center_z", "fixed_angle", "total_structures", "structure_index"
            ).map(ops::createString);
        }

        @Override
        public <T> DataResult<RingConfig> decode(DynamicOps<T> ops, MapLike<T> input) {
            T distVal = input.get(ops.createString("distance"));
            T countVal = input.get(ops.createString("count"));

            if (distVal == null) {
                return DataResult.error(() -> "[RadialPlacements] Missing required field 'distance'");
            }
            if (countVal == null) {
                return DataResult.error(() -> "[RadialPlacements] Missing required field 'count'");
            }

            return Codec.INT.parse(ops, distVal).flatMap(dist ->
                    Codec.INT.parse(ops, countVal).map(cnt -> {
                        RingConfig config = new RingConfig(
                                readOptionalString(ops, input, "label", "unnamed"),
                                dist, cnt,
                                readOptionalInt(ops, input, "spread_tolerance", 3),
                                readOptionalInt(ops, input, "distance_variance", 50),
                                readOptionalInt(ops, input, "center_x", 0),
                                readOptionalInt(ops, input, "center_z", 0),
                                readOptionalFloat(ops, input, "fixed_angle", -1.0f),
                                readOptionalInt(ops, input, "total_structures", 1),
                                readOptionalInt(ops, input, "structure_index", 0)
                        );
                        LOGGER.debug("[RadialPlacements] Decoded '{}': distance={}, count={}, spread={}, variance={}, center=({},{}), angle={}, total={}, index={}",
                                config.label, config.distance, config.count, config.spreadTolerance, config.distanceVariance,
                                config.centerX, config.centerZ, config.fixedAngle, config.totalStructures, config.structureIndex);
                        return config;
                    })
            );
        }

        @Override
        public <T> RecordBuilder<T> encode(RingConfig input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            prefix.add("label", ops.createString(input.label()));
            prefix.add("distance", ops.createInt(input.distance()));
            prefix.add("count", ops.createInt(input.count()));
            prefix.add("spread_tolerance", ops.createInt(input.spreadTolerance()));
            prefix.add("distance_variance", ops.createInt(input.distanceVariance()));
            prefix.add("center_x", ops.createInt(input.centerX()));
            prefix.add("center_z", ops.createInt(input.centerZ()));
            prefix.add("fixed_angle", ops.createFloat(input.fixedAngle()));
            prefix.add("total_structures", ops.createInt(input.totalStructures()));
            prefix.add("structure_index", ops.createInt(input.structureIndex()));
            return prefix;
        }
    };

    // Main codec: placementCodec (P5) + RingConfig (App) = P6, well within DFU limits
    public static final MapCodec<EquidistantRingPlacement> CODEC;

    static {
        try {
            CODEC = RecordCodecBuilder.mapCodec(instance ->
                    placementCodec(instance)
                            .and(RING_CONFIG_CODEC.forGetter(p -> p.config))
                            .apply(instance, EquidistantRingPlacement::new)
            );
            LOGGER.debug("[RadialPlacements] CODEC initialized successfully");
        } catch (Exception e) {
            LOGGER.error("[RadialPlacements] CODEC initialization FAILED", e);
            throw e;
        }
    }

    private final RingConfig config;

    public EquidistantRingPlacement(
            Vec3i locateOffset,
            FrequencyReductionMethod frequencyReductionMethod,
            float frequency,
            int salt,
            Optional<ExclusionZone> exclusionZone,
            RingConfig config
    ) {
        super(locateOffset, frequencyReductionMethod, frequency, salt, exclusionZone);
        this.config = config;
        LOGGER.debug("[RadialPlacements] Loaded '{}': salt={}, distance={}, count={}, center=({}, {}), totalStructures={}, structureIndex={}",
                config.label, salt, config.distance, config.count, config.centerX, config.centerZ, config.totalStructures, config.structureIndex);
    }

    @Override
    public StructurePlacementType<?> type() {
        return ModStructurePlacements.EQUIDISTANT_RING.get();
    }

    @Override
    protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int chunkX, int chunkZ) {
        long worldSeed = state.getLevelSeed();

        // Deterministic RNG seeded with world seed + placement salt
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
        random.setLargeFeatureSeed(worldSeed, 0, salt());

        // Determine base rotation angle
        double initialAngle;
        if (config.fixedAngle >= 0.0f) {
            initialAngle = Math.toRadians(config.fixedAngle);
        } else {
            initialAngle = random.nextDouble() * 2 * Math.PI;
        }

        boolean shouldLog = !this.hasLoggedPositions;
        if (shouldLog) {
            this.hasLoggedPositions = true;
            LOGGER.info("[RadialPlacements] Computing ring '{}': salt={}, distance={}, count={}, center=({}, {}), totalStructures={}, structureIndex={}",
                    config.label, salt(), config.distance, config.count, config.centerX, config.centerZ, config.totalStructures, config.structureIndex);
        }

        // Evaluate each target node in the ring
        // Always compute ALL nodes to keep RNG state synchronized across round-robin placements
        for (int i = 0; i < config.count; i++) {
            double currentAngle = initialAngle + (i * (2.0 * Math.PI / config.count));

            // Apply distance variance
            int variedDistance = config.distance;
            if (config.distanceVariance > 0) {
                variedDistance += random.nextInt(config.distanceVariance * 2 + 1) - config.distanceVariance;
            }

            // Polar to Cartesian block coordinates
            int targetBlockX = config.centerX + (int) (variedDistance * Math.cos(currentAngle));
            int targetBlockZ = config.centerZ + (int) (variedDistance * Math.sin(currentAngle));

            // Block coordinates to chunk coordinates (>> 4 divides by 16)
            int targetChunkX = targetBlockX >> 4;
            int targetChunkZ = targetBlockZ >> 4;

            // Apply spread tolerance via deterministic sub-chunk scattering
            if (config.spreadTolerance > 0) {
                // Re-seed RNG based on the target chunk for deterministic scatter
                random.setLargeFeatureSeed(worldSeed, targetChunkX, targetChunkZ);
                int offsetX = random.nextInt(config.spreadTolerance * 2 + 1) - config.spreadTolerance;
                int offsetZ = random.nextInt(config.spreadTolerance * 2 + 1) - config.spreadTolerance;
                targetChunkX += offsetX;
                targetChunkZ += offsetZ;
            }

            boolean isMyNode = config.totalStructures <= 1 || (i % config.totalStructures) == config.structureIndex;

            if (shouldLog && isMyNode) {
                LOGGER.info("[RadialPlacements]   '{}' Node {} -> block({}, ~, {}) chunk({}, {}) angle={}deg dist={}",
                        config.label, i, targetChunkX * 16 + 8, targetChunkZ * 16 + 8, targetChunkX, targetChunkZ,
                        String.format("%.1f", Math.toDegrees(currentAngle) % 360), variedDistance);
            }

            // Round-robin: only match nodes assigned to this structure index
            if (!isMyNode) {
                continue;
            }

            if (chunkX == targetChunkX && chunkZ == targetChunkZ) {
                return true;
            }
        }

        return false;
    }

    // Helper methods for hand-written codec
    private static <T> int readOptionalInt(DynamicOps<T> ops, MapLike<T> input, String name, int defaultVal) {
        T val = input.get(ops.createString(name));
        if (val == null) return defaultVal;
        return Codec.INT.parse(ops, val).result().orElse(defaultVal);
    }

    private static <T> String readOptionalString(DynamicOps<T> ops, MapLike<T> input, String name, String defaultVal) {
        T val = input.get(ops.createString(name));
        if (val == null) return defaultVal;
        return Codec.STRING.parse(ops, val).result().orElse(defaultVal);
    }

    private static <T> float readOptionalFloat(DynamicOps<T> ops, MapLike<T> input, String name, float defaultVal) {
        T val = input.get(ops.createString(name));
        if (val == null) return defaultVal;
        return Codec.FLOAT.parse(ops, val).result().orElse(defaultVal);
    }

    // Compound record for all custom ring parameters
    public record RingConfig(
            String label,
            int distance,
            int count,
            int spreadTolerance,
            int distanceVariance,
            int centerX,
            int centerZ,
            float fixedAngle,
            int totalStructures,
            int structureIndex
    ) {}
}
