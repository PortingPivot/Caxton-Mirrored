# Caxton

**Caxton**, named after [William Caxton], is a Minecraft mod that adds TrueType and OpenType font support.

## Features

* Crisp text at any size thanks to [MSDF] technology
* Real bold and italic fonts
* Complex text rendering
* Does not use AWT

### Current limitations

* Generating MTSDFs from fonts is expensive; it takes about 3 minutes to load 4 families of Inter. For this reason,
  Caxton will cache the results after this is first done.
* Underline and strikethrough styles are not yet implemented for Caxton text.
* Most text handler calculations are not yet aware of text in Caxton fonts.
* No font hinting. Uncertain whether this will ever be supported.
* Glowing sign text is not yet supported.
* This mod currently only runs on Linux, although in principle, support for other operating systems can be added,
  including macOS.
* Currently, all glyphs are uploaded to VRAM eagerly, and while shaping results are cached, they are cached
  indefinitely.

## How to use Caxton

Caxton currently comes with two built-in resource packs for fonts. The first font included is [Inter], while the second
is [Open Sans].

Caxton adds a font provider of type `caxton`, which supports the keys `regular`, `bold`, `italic`, and `bold_italic`.
Each of these can be set to an identifier, where `<namespace>:<path>` resolves
to the font file `assets/<namespace>/textures/font/<path>`. To specify other options, use an object where the
key `file` specifies the path:

```json5
{
  // The only required element.
  "file": "<namespace>:<path>",
  // The shadow offset, as a multiple of the memefont pixel size.
  "shadow_offset": 0.5,
  // A list of OpenType feature tags. See below for the syntax:
  // https://docs.rs/rustybuzz/0.6.0/rustybuzz/struct.Feature.html#method.from_str
  "features": [],
}
```

You also need to add the file `assets/<namespace>/textures/font/<path>.json`, which contains settings for
rasterizing the font:

```json5
{
  // Specifies the actual path of the font file, as it would appear in the Caxton
  // font provider.
  // This should generally be omitted, but can be useful if you are using a
  // variable font.
  "path": "<path of font file>",
  // All of these options are optional and will default to the provided values.
  // The number of font units corresponding to each pixel in the texture atlas.
  // This can be set to a high value because the atlas is an MTSDF.
  "shrinkage": 32.0,
  // The number of pixels to leave around the glyph bounding box on each side.
  // This should be larger than `range`.
  "margin": 8,
  // The width of the range around the glyph between the minimum and maximum
  // representable signed distances.
  "range": 4,
  // Whether to invert the signed distance field.
  // If your glyphs appear inverted, then try changing this setting.
  "invert": false,
  // The size of each page in the texture atlas.
  "page_size": 4096,
  // This option is used to set variation axis coordinates in variable fonts.
  // Each element has the following format:
  // { "axis": <axis type>, "value": <axis value> }
  "variations": []
}
```

[William Caxton]: https://en.wikipedia.org/wiki/William_Caxton

[MSDF]: https://github.com/Chlumsky/msdfgen

[Inter]: https://github.com/rsms/inter

[Open Sans]: https://github.com/googlefonts/opensans

## Comparison with other mods

### BetterFonts / TrueType Font Replacement

Originally created by [thvortex] through 1.4.4, updated to 1.4.7 by bechill, then to 1.5.2 by [The_MiningCrafter], then
by [secretdataz] to 1.6.x and 1.7.x, then by [cubex2] from 1.8.9 to 1.12.2. Then updated to 1.13 again by secretdataz.

This mod uses Java AWT’s text layout functionality for laying out text. For rendering the glyphs, it rasterizes them
into bitmaps. The resolution is quite limited. Unlike the other mods listed below, however, it implements bold and
italic styles, as well as complex scripts, properly.

[thvortex]: https://github.com/thvortex/BetterFonts

[The_MiningCrafter]: https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/minecraft-mods/1287298-1-5-1-betterfonts-make-your-minecraft-chat-text

[secretdataz]: https://github.com/secretdataz/BetterFonts

[cubex2]: https://github.com/cubex2/BetterFonts

### Smooth Font

Created by [bre2el] for versions from 1.7 to 1.12. This mod also improves rendering of text at different scales and
implements some optimizations for text rendering.

As for how it works, who the `RenderType` knows? This mod is ARR.

[bre2el]: https://www.curseforge.com/minecraft/mc-mods/smooth-font

### ThaiFixes

Created by [lion328] for Forge on versions up to 1.12.2 and for Rift on 1.13, and updated through 1.18.2 on Fabric
by [secretdataz][secretdataz2].

This mod implements its own shaping routines for Thai specifically. Thus, it is not useful for other languages that
require complex rendering.

[lion328]: https://github.com/lion328/thaifixes

[secretdataz2]: https://github.com/secretdataz/ThaiFixes-Fabric

### Modern UI

Created by [BloCamLimb] for versions 1.15 to 1.19 on Forge.

From the screenshots, it seems that this mod supports complex text rendering and true bold and italic styles. It also
fixes many issues with vanilla text layout such as [MC-117311].

Judging by the code, Modern UI has a surprisingly sophisticated layout algorithm. I haven’t had much time to look at it,
though.

However, this mod fails to render text with crisp borders. It also uses AWT for performing text layout.

[BloCamLimb]: https://github.com/BloCamLimb/ModernUI

[MC-117311]: https://bugs.mojang.com/browse/MC-117311

### Minecraft 1.13 and later

Since 1.13, Minecraft supports TrueType and OpenType fonts. However, this implementation is not fundamentally different
from those of bitmap fonts – the game converts the glyphs into bitmaps and lays out text naïvely. In addition, it
handles glyph metrics incorrectly, causing TTF text to appear off-kilter.
