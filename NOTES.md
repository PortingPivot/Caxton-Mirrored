# Notes on Minecraft’s text rendering stack

(Here, ‘legacy’ fonts refer to any fonts supported by vanilla Minecraft,
not just the `legacy_unicode` fonts.)

We’ll probably have to make major changes to the stack.

We can probably mixin to `FontStorage` to never ignore `CaxtonTypeface`s.
These would subsequently be ignored when being queried for glyphs since
`CaxtonTypeface` always returns null for `getGlyph`, so we can use
`FontStorage` for rendering ‘legacy’ fonts. We can also mixin a method
to return the `CaxtonTypeface` that would be used for a given codepoint,
or else the glyph used if it would be drawn in a legacy font – ideally
with caching.

We can then split the text into chunks that can be shaped together.
For chunks that use legacy fonts, we perform the same calculations
as vanilla Minecraft for text shaping and width calculation. For chunks
that use Caxton fonts, we do our own calculations for these. These
calculations would be done in Rust using rustybuzz, returning an
instance of `ShapingResult` through JNI.

Glyphs from Caxton fonts are stored as MTSDFs and need special rendering.
Unfortunately, this isn’t an area I’m terribly familiar with.

## Atlas generation

Atlas generation works but is suboptimal. What if we sorted the glyphs
by descending height?
