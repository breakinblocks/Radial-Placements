# Radial Placements

A NeoForge 1.21.1 mod that adds the `equidistant_ring` structure placement type — place structures in configurable circular formations around any center point.

## Features

- Place a fixed number of structures evenly spaced in a ring at a configurable radius
- Deterministic placement seeded by world seed — same seed always produces the same ring
- Round-robin mode: assign different structures to specific nodes in the same ring
- Configurable center point, rotation angle, distance variance, and spread tolerance
- Works with datapacks and mod-provided structure sets
- Debug logging shows exact placement coordinates on first world load

## Installation

Drop the mod JAR into your `mods/` folder. Requires NeoForge for Minecraft 1.21.1.

## Placement Type

`radial_placements:equidistant_ring`

## Configuration

Structure sets are defined as JSON files at:

```
data/<namespace>/worldgen/structure_set/<name>.json
```

### Basic Example

Place 6 villages in a ring 500 blocks from world origin:

```json
{
  "structures": [
    { "structure": "minecraft:village_plains", "weight": 1 },
    { "structure": "minecraft:village_desert", "weight": 1 },
    { "structure": "minecraft:village_savanna", "weight": 1 },
    { "structure": "minecraft:village_snowy", "weight": 1 },
    { "structure": "minecraft:village_taiga", "weight": 1 }
  ],
  "placement": {
    "type": "radial_placements:equidistant_ring",
    "label": "villages",
    "salt": 74029831,
    "distance": 500,
    "count": 6
  }
}
```

> **Note:** Include multiple biome variants of a structure so the ring works across different biomes. A structure silently fails if its target chunk has an incompatible biome.

### Placement Fields

#### Required

| Field | Type | Description |
|-------|------|-------------|
| `type` | string | Must be `"radial_placements:equidistant_ring"` |
| `salt` | int | Unique seed salt. Use a different salt per structure set to avoid overlap. |
| `distance` | int | Radius in blocks from center to ring nodes. |
| `count` | int | Number of evenly-spaced positions around the ring. |

#### Optional

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `label` | string | `"unnamed"` | Identifier shown in log output for this placement. |
| `spread_tolerance` | int | `3` | Chunk scatter radius around each node. Higher values help structures find valid biomes. |
| `distance_variance` | int | `50` | Max block deviation from `distance`. Each node randomly shifts closer/farther by up to this amount. |
| `center_x` | int | `0` | Ring center X in block coordinates. |
| `center_z` | int | `0` | Ring center Z in block coordinates. |
| `fixed_angle` | float | `-1.0` | Starting angle in degrees. Negative = randomized per world seed. Set `0.0`+ to lock orientation. |
| `total_structures` | int | `1` | Round-robin mode: how many different structure sets share this ring. |
| `structure_index` | int | `0` | Round-robin mode: which slice of nodes this placement handles (0-based). |
| `frequency` | float | `1.0` | Probability (0-1) each node actually attempts generation. |
| `frequency_reduction_method` | string | `"default"` | One of: `default`, `legacy_type_1`, `legacy_type_2`, `legacy_type_3`. |
| `locate_offset` | [x,y,z] | `[0,0,0]` | Offset for `/locate` command results. |
| `exclusion_zone` | object | none | Prevents spawning near another structure set. Format: `{"other_set": "<id>", "chunk_count": <int>}` |

## Round-Robin Mode

To place **different structures at different positions** in the same ring, create one structure set file per structure type. All files share the same `salt`, `distance`, `count`, and ring parameters, but each gets a unique `structure_index`.

### Example: 3 structures at 120 degrees apart

**ring_village.json** — Node 0:
```json
{
  "structures": [{ "structure": "minecraft:village_plains", "weight": 1 }],
  "placement": {
    "type": "radial_placements:equidistant_ring",
    "label": "village",
    "salt": 74029831,
    "distance": 300,
    "count": 3,
    "spread_tolerance": 0,
    "distance_variance": 0,
    "total_structures": 3,
    "structure_index": 0
  }
}
```

**ring_outpost.json** — Node 1:
```json
{
  "structures": [{ "structure": "minecraft:pillager_outpost", "weight": 1 }],
  "placement": {
    "type": "radial_placements:equidistant_ring",
    "label": "outpost",
    "salt": 74029831,
    "distance": 300,
    "count": 3,
    "spread_tolerance": 0,
    "distance_variance": 0,
    "total_structures": 3,
    "structure_index": 1
  }
}
```

**ring_portal.json** — Node 2:
```json
{
  "structures": [{ "structure": "minecraft:ruined_portal", "weight": 1 }],
  "placement": {
    "type": "radial_placements:equidistant_ring",
    "label": "portal",
    "salt": 74029831,
    "distance": 300,
    "count": 3,
    "spread_tolerance": 0,
    "distance_variance": 0,
    "total_structures": 3,
    "structure_index": 2
  }
}
```

The `salt`, `distance`, `count`, `distance_variance`, `spread_tolerance`, `center_x`, `center_z`, and `fixed_angle` **must be identical** across all files sharing a ring so node positions stay synchronized.

### How Round-Robin Node Assignment Works

With `count: 6` and `total_structures: 3`:

| Node | Angle | structure_index | Structure |
|------|-------|-----------------|-----------|
| 0 | 0 | 0 | Village |
| 1 | 60 | 1 | Outpost |
| 2 | 120 | 2 | Portal |
| 3 | 180 | 0 | Village |
| 4 | 240 | 1 | Outpost |
| 5 | 300 | 2 | Portal |

Assignment formula: `node_index % total_structures == structure_index`

## Log Output

On first chunk generation, the mod logs ring node positions at INFO level:

```
[RadialPlacements] Computing ring 'village': salt=74029831, distance=300, count=3, ...
[RadialPlacements]   'village' Node 0 -> block(-232, ~, 200) chunk(-15, 12) angle=139.6deg dist=300
```

Use these coordinates to locate or verify structure placement. Additional codec/loading details are logged at DEBUG level.

## Tips

- **Biome coverage:** Include all biome variants of a structure (e.g., all village types, all ruined portal types) so nodes work across biomes.
- **Superflat testing:** Use `minecraft:village_plains` or `minecraft:pillager_outpost` on superflat worlds — the entire world is plains biome.
- **Perfect circles:** Set `distance_variance: 0` and `spread_tolerance: 0` for exact geometric placement.
- **Reasonable count:** Keep `count` under 128. Each node is evaluated per generated chunk.
- **Unique salts:** Every structure set needs a unique `salt` value. Reusing salts causes rings to overlap — **except** round-robin sets which must share the same salt.
- **Structure selection:** Within a single structure set file, Minecraft picks randomly from the `structures` array by weight. Use round-robin mode for deterministic structure-to-node assignment.

## Pack Format

For use in datapacks with Minecraft 1.21.1, use `pack_format: 48`:

```json
{
  "pack": {
    "pack_format": 48,
    "description": "My ring structures"
  }
}
```

## Building

```bash
./gradlew build
```

Output JAR is in `build/libs/`.

## License

GNU GPL 3.0
