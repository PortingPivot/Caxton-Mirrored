# Caxton

**Caxton**, named after [William Caxton], is a Minecraft mod that adds TrueType and OpenType font support.

## Features

* Crisp text at any size thanks to [MSDF] technology
* Real bold and italic fonts
* Complex text rendering

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
Each of these can be set to an identifier, where `\<namespace\>:\<path\>` resolves
to the font file `assets/\<namespace\>/textures/font/\<path\>`. To specify other options, use an object where the
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

You also need to add the file `assets/\<namespace\>/textures/font/\<path\>.json`, which contains settings for
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
