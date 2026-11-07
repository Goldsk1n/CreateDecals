# Changelog

## 1.0.0 (unreleased)

First release, for Minecraft 1.21.1 with NeoForge 21.1.125+ and Create 6.0.0 to 6.0.x.

### Blocks and items

- **Decal Panel**: Create's Copycat Panel with a decal on its front. It copies any block's look as the Copycat Panel
  does; crafted from a Copycat Panel (and back). Create's own Copycat Panels already in a build need no swapping: a
  stencil click on one's front turns it into a Decal Panel in place, keeping its material, facing and the block used on
  it, then puts the decal on.
- **Decal Block**: a full copycat block with a decal on each of its faces, for hulls built of whole blocks. Faces against
  a solid block are hidden, decal and all. Cut from a zinc ingot on a Stonecutter.
- **Decal Stencil**: used in the air it opens a screen to choose a decal, its colour (the 16 dyes or a hex code) and how
  it is turned and flipped; on a decal block's face it puts the decal there. Sneak-clicking a decal picks it up into the
  stencil; its eraser takes a decal off. While held, it outlines the faces its decal would cover: green where a tile
  lands, red where it cannot. Its icon shows the decal it holds. Crafted from an Iron Sheet, paper and a dye.

### Decals

- 77 built-in decals in Create's style: 26 letters and 10 digits (a 16 px pixel font), 6 signs, 12 lines (stripes,
  hazard stripes, chequer), 9 markers (arrows, chevrons, frames) and 14 emblems (cogs, a train, a balloon, a propeller,
  wings and more). 7 of them cover several blocks, up to 3x3.
- Decals larger than one block spread right and down from the face you click, in one click, over the decal blocks
  beside it, turned and flipped as a whole. The eraser and dye work on any tile and change the whole decal.
- A dye clicked on a decal recolours it. Create's Clipboard copies a block's decals to another.
- Resource packs can add decals: a definition at `assets/<namespace>/decals/<name>.json` (size in blocks and a
  category) and its picture at `textures/decal/<name>.png`. A client without that pack shows nothing there.

### Where decals work

- Decals are part of the block's model, drawn on the face itself: crisp on opaque, cutout and translucent materials,
  with no flicker near or far.
- They stay on Create contraptions, trains and Sable vehicles (Create Aeronautics), and are put back turned with the
  block when a contraption stops at an angle. Schematics and the Schematicannon keep them.
- Works with Sodium and Iris shader packs; no measurable frame cost against Create's own copycats, at 1000 blocks.
- Every block and the stencil have a Ponder scene.
