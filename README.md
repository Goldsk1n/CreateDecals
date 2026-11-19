# Create: Decals

Decals for [Create](https://modrinth.com/mod/create)'s copycat blocks.

**Download:** [CurseForge](https://www.curseforge.com/minecraft/mc-mods/create-decals). Changes: [CHANGELOG.md](CHANGELOG.md).
**Community and support:** [Discord](https://discord.gg/HAtMB7UQss). Bugs: [GitHub Issues](https://github.com/Goldsk1n/CreateDecals/issues).

## Blocks and items

| | What it does |
| --- | --- |
| Decal Block | A full copycat block with a decal on each face |
| Create's Copycat Panel and Copycat Step | Take decals as they are; a step's faces each show the part of the decal they cover |
| Decal Stencil | Picks a decal, a typed word, a colour, a turn and flip, a layer and glow in its screen; puts it on with a click, picks one up with a sneak-click, erases, undoes (Z) |

Decal Blocks come off the Stonecutter from zinc, as Create's copycats do. Create's own Copycat Panels and Steps stay
Create's blocks: their decals are kept beside them, so a build shared with someone without this mod loads as plain
copycats. Putting decals on costs nothing.

- 101 built-in decals in Create's style, decals up to 3x3 blocks laid in one click, two decals on one face, typed words.
- Any colour (the 16 dyes or a hex code); a dye on a decal recolours it, a Glow Ink Sac makes it glow at night.
- Create's Deployer uses the stencil too, standing or carried on a contraption; Create's Clipboard copies decals.
- Resource packs can add decals: `assets/<namespace>/decals/<name>.json` (`"size": [w, h]` in blocks, at most 4 x 4,
  and a `"category"`) with its picture at `assets/<namespace>/textures/decal/<name>.png`.
- With Copycats+ (3.0.1 or newer), its copycats take decals too, slanted faces included.

## How it works

A decal is part of the copycat's baked model: one more quad, coplanar with the face it lies on and drawn in the cutout
layer after it, so it wins the depth test. Decals therefore render wherever the block renders, with no render hooks: on
contraptions, trains and Sable vehicles, under Sodium and Iris shader packs, in schematics. The block entity keeps each
face's decals (two layers) and turns them with the block when a contraption puts it back at an angle. On Copycats+'s
copycats the decals live in a block entity attachment that the mod syncs itself.

## Requirements

| | Versions |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.125 or newer |
| Create | 6.0.0 or newer, below 6.1.0 |
| Optional | Sable with Create Aeronautics; Copycats+ 3.0.1 or newer |

Needed on both the server and the clients. Tested with Sodium 0.8, Iris 1.8, Entity Culling, ImmediatelyFast and
Sodium Extra.

## Building

Requires JDK 21.

```bash
./gradlew build                                  # jar in build/libs; runs unit, architecture and content tests
./gradlew runData                                # regenerate src/generated (blockstates, lang, recipes, loot, Ponder schematics)
./gradlew runGameTestServer                      # headless game tests
./gradlew runGameTestServer -Prun_profile=floor  # the same on NeoForge 21.1.125 and Create 6.0.0
./gradlew runClient                              # dev client
```

`-Pdev_copycats=true` adds Copycats+ to dev runs and game tests, `-Pdev_perf=true` Sodium and Iris, `-Pdev_tools=true`
JEI and The One Probe.

## Licence

[MIT](LICENSE).
