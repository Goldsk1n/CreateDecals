# Changelog

## 1.0.0 (unreleased)

First release, for Minecraft 1.21.1 with NeoForge 21.1.125+ and Create 6.0.0 to 6.0.x.

### Blocks and items

- **Decal Panel**: Create's Copycat Panel with a decal on its front. It copies any block's look as the Copycat Panel
  does; crafted from a Copycat Panel (and back). Create's own Copycat Panels already in a build need no swapping: a
  stencil click on one's front turns it into a Decal Panel in place, keeping its material, facing and the block used on
  it, then puts the decal on.
- **Decal Block**: a full copycat block with a decal on each of its faces, for hulls built of whole blocks. Faces against
  a solid block are hidden, decal and all. Two from a zinc ingot on a Stonecutter.
- **Decal Step**: Create's Copycat Step with a decal on any of its faces, for trims and ledges. Each face shows the part
  of the decal it covers, as if the decal lay on the whole face of the block in that plane, so stripes and patterns run
  on unbroken from the Decal Blocks beside it. Crafted from a Copycat Step (and back); Create's own Copycat Steps turn
  into Decal Steps in place when a stencil puts a decal on any of their faces.
- **Decal Stencil**: used in the air it opens a screen to choose a decal or type a word, its colour (the 16 dyes or a
  hex code), how it is turned and flipped, its layer and whether it glows; on a decal block's face it puts the decal
  there. Sneak-clicking a decal picks it up into the stencil; its eraser takes a decal off. Its icon shows the decal it
  holds. Crafted from an Iron Sheet, paper and a dye.
- While a stencil is held, a see-through ghost shows its decal or word where a click would put it, in its colour, turn
  and flip, outlined green; red outlines mark tiles that would miss. With the eraser, the decal it would take off is
  drawn over in red, so on a face with two decals it shows which one goes. Aimed at a block that takes no decals, the
  face is outlined red, and clicking it opens the screen with a note on where decals go.
- Ctrl + scroll while holding a stencil turns its decal or word a quarter, without opening the screen.
- Z (rebindable in Controls) undoes the last stencil click: a decal, a large one, a word or the eraser, up to 10 clicks
  back. Tiles painted over since, mined, carried off on a contraption or now protected are left alone, and the action
  bar says how many came back. The history is kept until the player leaves.

### Decals

- 77 built-in decals in Create's style: 26 letters and 10 digits (a 16 px pixel font), 6 signs, 12 lines (stripes,
  hazard stripes, chequer), 9 markers (arrows, chevrons, frames) and 14 emblems (cogs, a train, a balloon, a propeller,
  wings and more). 7 of them cover several blocks, up to 3x3.
- Decals larger than one block spread right and down from the face you click, in one click, over the decal blocks
  beside it, turned and flipped as a whole. The eraser and dye work on any tile and change the whole decal.
- Two decals on one face: set the stencil to the front layer to put a second decal over the one already there, a
  number on a stripe or a letter on a hazard band. The stencil's layer also decides which decal it picks up and erases
  (the other one when that layer is bare), so the decal under a front one can be picked up or erased alone; dye and
  glow work on the decal on top.
- Type a word (up to 16 letters, digits, spaces and & - . # + /) in the stencil's screen and one click lays it along
  the hull, one letter per block, turned as a whole; words are never mirrored.
- The stencil's screen names each decal when the mouse is on it (resource packs can name theirs with a
  `decal.<namespace>.<name>` language key).
- A Glow Ink Sac clicked on a decal makes it glow, full-bright at night, also on contraptions, trains and Sable
  vehicles, with Sodium and with shader packs; an Ink Sac stops it. The stencil's screen can
  set glow too.
- A dye clicked on a decal recolours it. Create's Clipboard copies a face's decals (both layers) to another.
- Resource packs can add decals: a definition at `assets/<namespace>/decals/<name>.json` (size in blocks and a
  category) and its picture at `textures/decal/<name>.png`. A client without that pack shows nothing there. When packs
  bring more categories than fit, the stencil screen's tab row scrolls.
- Large decals, words, the eraser, dye and glow change only blocks the player may build on: spawn protection, adventure
  mode and claim mods are respected for every block a click reaches, not only the one clicked.

### Where decals work

- Decals are part of the block's model, drawn on the face itself: crisp on opaque, cutout and translucent materials,
  with no flicker near or far.
- They stay on Create contraptions, trains and Sable vehicles (Create Aeronautics), and are put back turned with the
  block when a contraption stops at an angle. Schematics and the Schematicannon keep them.
- Works with Sodium and Iris shader packs; no measurable frame cost against Create's own copycats, at 1000 blocks.
- **Copycats+** (optional): its copycats take decals too, without converting: slopes, stairs, slabs, beams, vertical
  steps, slope layers, doors and the rest (not its turning shafts and cogwheels). A decal lies on every part of the shape
  that faces the clicked side, slanted faces included, as if it lay on the block's face in that plane. Stencil, ghost,
  dye, glow ink, Create's Clipboard (also to and from our blocks) and undo work on them; they keep their decals when
  saved, on contraptions and trains (glowing ones full-bright, also under Sodium), on Sable vehicles, in schematics, and
  when a contraption puts them back, turned with the block. Uses only Copycats+'s public `foundation`
  package and Create's own API (Copycats+ 2.0 and later; tested with 3.0.9).
- Every block and the stencil have a Ponder scene.
