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
Unfortunately, this isn’t an area I’m terribly familiar with. I think
I need to create some new render layers for Caxton text rendering with
MTDSF shaders. This requires storing information about whether each
quad represents the glyph itself or its shadow.

For outlined text, we need the text color and the border color for each
rect.

## Atlas generation

~~Atlas generation works but is suboptimal. What if we sorted the glyphs
by descending height?~~

## Other todos

~~Provide the font test as a built-in resource pack instead of overriding
the default font.~~

## `TextHandler` functions and their uses

* `int getTrimmedLength(String text, int maxWidth, Style style)`: returns the length of a string when trimmed to be at
  most `maxWidth` wide.
    * Used in the book editor to calculate the position for the cursor to jump to when the mouse is clicked or dragged.
    * Also used in `trimToWidth`.
* `String trimToWidth(String text, int maxWidth, Style style)`: trims a string to be at most `maxWidth` wide.
    * Used in similarly named methods in `TextHandler`, which are called by the following:
        * `TextFieldWidget#setSelectionEnd(int index)` to figure out where to horizontally scroll to
        * `EditBox#moveCursorLine` and `moveCursor` to determine the position in the string that corresponds to a screen
          position
        * `TextFieldWidget#mouseClicked`, also to calculate string positions.
            * As a sidenote, text edit boxes will need a major rework to function properly with right-to-left text.
              Vanilla Minecraft just throws its hands up and always renders the text left to right. (But see [MC-149453]
              for chat input when the game is in a right-to-left language.)
        * In `AdvancementWidget`, `PackListWidget`, and `ChatSelectionScreen` to prevent text from overflowing a box.

It would be ideal for RTL text to appear right-aligned, but alas, see [MC-117311].

[MC-117311]: https://bugs.mojang.com/browse/MC-117311
[MC-149453]: https://bugs.mojang.com/browse/MC-149453

