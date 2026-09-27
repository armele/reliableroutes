# Modpack Customization

Reliable Routes exposes a small set of server configuration and data-pack tags for modpack authors. These can be changed without rebuilding the mod.

Examples below use a data pack named `my_pack`. Put it in a world's `datapacks` directory, or ship the same `data` tree through a mod or global data-pack loader.

## Enable or Disable Custom Navigation

The server config file is `reliableroutes-server.toml`. Set:

```toml
enableCustomPathfinding = true
```

When this is `false`, entities use their normal MineColonies navigation. Path Pair blocks can still be crafted and paired, and existing routing zones remain saved, but the routes do not affect pathfinding.

NeoForge server configs are normally stored with the world under `serverconfig`. A pack may also provide its preferred initial file through the usual NeoForge `defaultconfigs` mechanism. Restart the server after changing this option.

## Add Entity Types to Reliable Routes Navigation

Entity types eligible for the custom navigator are selected by the entity-type tag:

```text
reliableroutes:uses_reliable_routes_navigator
```

Reliable Routes includes `minecolonies:citizen` by default. To add more entity types, create:

```text
my_pack/
  pack.mcmeta
  data/
    reliableroutes/
      tags/
        entity_type/
          uses_reliable_routes_navigator.json
```

For example:

```json
{
  "replace": false,
  "values": [
    "examplemod:worker",
    "examplemod:porter",
    "#examplemod:route_users"
  ]
}
```

Use `"replace": false` to keep the built-in MineColonies citizen entry. A value beginning with `#` includes another entity-type tag.

There is an important compatibility constraint: Reliable Routes supplies its navigator through the MineColonies path-navigator registry. The entity must be a `Mob` and must participate compatibly in that navigation system. Adding an arbitrary vanilla or third-party entity type to the tag does not inject MineColonies navigation support into it, and may have no effect. Test each added entity in game in both ordinary movement and Path Pair crossings.

After changing entity tags, reload the data packs and recreate or reload the affected entities; a full server restart is the safest way to ensure navigators are reconstructed.

## Customize Forbidden Ground

Blocks treated as Reliable Routes forbidden ground are selected by:

```text
reliableroutes:forbidden_ground
```

Add entries with `data/reliableroutes/tags/block/forbidden_ground.json`:

```json
{
  "replace": false,
  "values": [
    "examplemod:hazard_tiles",
    "#examplemod:unsafe_flooring"
  ]
}
```

The built-in tag contains `reliableroutes:forbidden_ground` and `reliableroutes:forbidden_ground_curb`. Reliable Routes also includes this entire tag in MineColonies' `minecolonies:dangerousblocks` tag, so additions are exposed to MineColonies as dangerous blocks and appear as dangerous in the Pathfinder Lens overlay.

The entity-type tag `reliableroutes:traverse_forbidden_ground` is present as a reserved compatibility point, but it does not currently grant an exception to dangerous-ground pathing. Adding entities to it has no behavioral effect in the current release.

## Customize Routing Block Materials

The blocks that may be used as appearance materials for Reliable Routes' materially textured blocks are controlled by:

```text
reliableroutes:routing_block_materials
```

Add entries with `data/reliableroutes/tags/block/routing_block_materials.json`:

```json
{
  "replace": false,
  "values": [
    "examplemod:polished_road_stone",
    "#examplemod:road_materials"
  ]
}
```

This affects the materials offered for the road, forbidden-ground, curb, and Path Pair blocks through Domum Ornamentum. Not every unusual block model or render type is guaranteed to look correct, so verify custom materials both in inventory and when placed.

## Extend MineColonies Road and Danger Tags Directly

Reliable Routes classifies its road and forbidden-ground blocks through standard MineColonies tags:

- `minecolonies:pathblocks` controls preferred path blocks.
- `minecolonies:dangerousblocks` controls dangerous blocks.

The built-in `minecolonies:pathblocks` tag includes `#reliableroutes:pathing`. That Reliable Routes tag
contains both the road and stair, and is the preferred extension point when a block should be treated as
part of the Reliable Routes road family as well as a MineColonies path block.

A pack can contribute other blocks to those tags in the same way. For example, place a file at `data/minecolonies/tags/block/pathblocks.json`:

```json
{
  "replace": false,
  "values": [
    "examplemod:custom_road"
  ]
}
```

These are MineColonies-wide classifications, not behavior unique to Reliable Routes.

## Replace Recipes, Loot, and Client Resources

Standard data-pack and resource-pack overrides can also customize the mod's content:

- Recipes are under `data/reliableroutes/recipe/`.
- Block loot tables are under `data/reliableroutes/loot_table/blocks/`.
- English names are under `assets/reliableroutes/lang/en_us.json`.
- Blockstates, models, and textures can be overridden with normal resource-pack precedence.

Use the same namespace and resource path as the built-in asset when replacing it. Recipe and loot-table directories are singular (`recipe` and `loot_table`) for the Minecraft version targeted by this mod.

## Deployment Notes

- Server configuration and data packs determine gameplay behavior and must be installed on the server.
- Resource-pack changes must be available to clients that should see them.
- Keep `replace` set to `false` when extending tags unless intentionally replacing every lower-priority entry.
- Test navigation changes in a disposable world before deploying them to an existing server. In particular, verify normal fallback movement, both directions through a Path Pair zone, entity behavior after a restart, and any interaction with water or dangerous blocks.
