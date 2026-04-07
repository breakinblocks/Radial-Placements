# Radial Placements - Datapack Format

## Placement Type

`radial_placements:equidistant_ring`

Places a fixed number of structures in a circular ring around a center point.

## Structure Set JSON

Location: `data/<namespace>/worldgen/structure_set/<name>.json`

```json
{
  "structures": [
    { "structure": "minecraft:village_plains", "weight": 1 },
    { "structure": "minecraft:village_desert", "weight": 1 }
  ],
  "placement": {
    "type": "radial_placements:equidistant_ring",
    "label": "my_ring",
    "salt": 74029831,
    "distance": 300,
    "count": 3
  }
}
```

## Placement Fields

### Required

| Field | Type | Description |
|-------|------|-------------|
| `type` | string | Must be `"radial_placements:equidistant_ring"` |
| `salt` | int (>= 0) | Unique seed salt. Use a different salt per structure set to avoid overlap. |
| `distance` | int | Radius in blocks from center to ring nodes. |
| `count` | int | Number of structures evenly spaced around the ring. |

### Optional

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `label` | string | `"unnamed"` | Identifier shown in log output. Helps identify which placement is which when debugging. |
| `spread_tolerance` | int | 3 | Chunk scatter radius around each target node. Higher values help structures find valid biomes. |
| `distance_variance` | int | 50 | Max block deviation from `distance`. Each node shifts randomly closer/farther by up to this amount. |
| `center_x` | int | 0 | Ring center X in block coordinates. |
| `center_z` | int | 0 | Ring center Z in block coordinates. |
| `fixed_angle` | float | -1.0 | Starting angle in degrees. If negative, the ring rotation is randomized per world seed. Set to `0.0` or above to lock orientation. |
| `frequency` | float (0-1) | 1.0 | Probability each node actually attempts generation. |
| `frequency_reduction_method` | string | `"default"` | One of: `default`, `legacy_type_1`, `legacy_type_2`, `legacy_type_3`. |
| `locate_offset` | [x, y, z] | [0, 0, 0] | Offset for `/locate` command results. |
| `exclusion_zone` | object | none | Prevents spawning near another structure set. Format: `{ "other_set": "<structure_set_id>", "chunk_count": <int 1-16> }` |
| `total_structures` | int | 1 | For round-robin mode: how many different structure sets share this ring. |
| `structure_index` | int | 0 | For round-robin mode: which slice of nodes this placement handles (0-based). |

## Round-Robin Mode

To place **different structures at different nodes** in the same ring, create one structure set JSON per structure type. All share the same `salt`, `distance`, `count`, and ring parameters, but each gets a unique `structure_index` with `total_structures` set to the number of structure types.

Example: 3 different structures in a 6-node ring (each appears twice):

**ring_villages.json** — handles nodes 0, 3:
```json
{
  "structures": [{ "structure": "minecraft:village_plains", "weight": 1 }],
  "placement": {
    "type": "radial_placements:equidistant_ring",
    "label": "village",
    "salt": 74029831,
    "distance": 500,
    "count": 6,
    "total_structures": 3,
    "structure_index": 0
  }
}
```

**ring_outposts.json** — handles nodes 1, 4:
```json
{
  "structures": [{ "structure": "minecraft:pillager_outpost", "weight": 1 }],
  "placement": {
    "type": "radial_placements:equidistant_ring",
    "label": "outpost",
    "salt": 74029831,
    "distance": 500,
    "count": 6,
    "total_structures": 3,
    "structure_index": 1
  }
}
```

**ring_pyramids.json** — handles nodes 2, 5:
```json
{
  "structures": [{ "structure": "minecraft:desert_pyramid", "weight": 1 }],
  "placement": {
    "type": "radial_placements:equidistant_ring",
    "label": "pyramid",
    "salt": 74029831,
    "distance": 500,
    "count": 6,
    "total_structures": 3,
    "structure_index": 2
  }
}
```

The `salt`, `distance`, `count`, `distance_variance`, `spread_tolerance`, `center_x`, `center_z`, and `fixed_angle` **must be identical** across all files sharing a ring so the node positions stay synchronized.

## Tips

- Include **multiple structure variants** (e.g. all village types) so the ring works across biomes. A structure silently fails if its target chunk has an invalid biome.
- Keep `count` reasonable (under 128). Each node is evaluated per chunk generated.
- Use higher `spread_tolerance` (5-10) for biome-restricted structures.
- Set `distance_variance` to `0` for a perfect geometric circle.
- Every structure set needs a **unique `salt`** value. Reusing salts causes rings to overlap — **except** for round-robin sets which must share the same salt.

## Pack Format

For Minecraft 1.21.1, use `pack_format: 48` in `pack.mcmeta`:

```json
{
  "pack": {
    "pack_format": 48,
    "description": "My ring structures"
  }
}
```
