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
* `String trimToWidth(String text, int maxWidth, Style style)`
  and `StringVisitable trimToWidth(StringVisitable text, int width, Style style)`: trims a string to be at
  most `maxWidth` wide.
    * Used in similarly named methods in `TextHandler`, which are called by the following:
        * `TextFieldWidget#setSelectionEnd(int index)` to figure out where to horizontally scroll to
        * `EditBox#moveCursorLine` and `moveCursor` to determine the position in the string that corresponds to a screen
          position
        * `TextFieldWidget#mouseClicked`, also to calculate string positions.
            * As a sidenote, text edit boxes will need a major rework to function properly with right-to-left text.
              Vanilla Minecraft just throws its hands up and always renders the text left to right. (But see [MC-149453]
              for chat input when the game is in a right-to-left language.)
        * In `AdvancementWidget`, `PackListWidget`, and `ChatSelectionScreen` to prevent text from overflowing a box.
    * Used to prevent phrases in the enchanting table from colliding with the displayed level cost.
* `String trimToWidthBackwards(String text, int maxWidth, Style style2)`: the same deal, but returns a suffix of the
  string instead of a prefix.
    * Used in `String TextHander#trimToWidth(String text, int maxWidth, boolean backwards)`, which is used
      in `TextFieldWidget#setSelectionEnd(int index)` to update `firstCharacterIndex`.
* `int getLimitedStringLength(String text, int maxWidth, Style style)`: the same shtick as `getTrimmedLength`, except
  that it uses `TextVisitFactory#visitFormatted` instead of `visitForwards`.
    * Used in `String limitString(String text, int maxWidth, Style style)`, which is not used anywhere else.
* `Style getStyleAt(StringVisitable text2, int x)`, `Style getStyleAt(OrderedText text, int x)`: gets the style at a
  given x-position on the screen.
    * Used in several places to determine what to show when hovering the cursor over some text.
    * Should be straightforward to reimplement.
* `int getEndingIndex(String text, int maxWidth, Style style)`: Gets the location in the string where the line would be
  broken. Not used.
* `static int moveCursorByWords(String text, int offset, int cursor, boolean consumeSpaceOrBreak)`: Moves a position in
  a string by a number of words. Used in text selection; can probably be left alone.
* `wrapLines(String text, int maxWidth, Style style, boolean retainTrailingWordSplit, LineWrappingConsumer consumer)`:
  Breaks text (interpreted with formatting codes) into wrapped lines and passes each line into a consumer.
    * Used by `List<StringVisitable> wrapLines(String text, int maxWidth, Style style2)`, which returns a list instead.
    * Used in various multi-line editing widgets for obvious reasons.
* `public void wrapLines(StringVisitable text2, int maxWidth, Style style2, BiConsumer<StringVisitable, Boolean> lineConsumer)`:
  Wraps `text2` and passes each visual line of the result into `lineConsumer`, along with whether it is a continuation
  of the previous logical line.
    * Implementation:
        * Collect all of the text’s nonempty styled strings into a list and create a `LineWrappingCollector` (hereafter
          ‘the collector’) out of it.
        * Set `bl2` and `bl3` to false.
        * Do the following (`block0`):
            * For each styled string in the collector’s parts:
                * If the string cannot fit in one line, then do the following:
                    * Let `i` be the index at which a new line would be started.
                    * Set `bl2` to true if the line break was caused by a newline, and false otherwise.
                    * Extract the first `i` characters from the collector, skipping the following character if it is a
                      space or newline.
                    * Pass the result and `bl3` into the line consumer.
                    * Set `bl3` if the line break was *not* caused by a newline, and false otherwise.
                    * Stop and retry the actions in `block0`.
                * Otherwise, update the line breaking visitor to contain the right indices.
        * Collect the remaining parts from the collectors, and pass the result and `bl3` into the line consumer.
            * If there are no remaining parts but the last line break was a newline, then accept an additional empty
              string as well as a false value.
    * Used by `List<StringVisitable> wrapLines(StringVisitable text2, int maxWidth, Style style)`
      and `List<StringVisitable> wrapLines(StringVisitable text2, int maxWidth, Style style, StringVisitable wrappedLinePrefix)`
      , the latter of which adds a prefix before every line before the last. Note that the latter method is completely
      unused.
    * Used by `List<OrderedText> TextRenderer#wrapLines(StringVisitable text, int width)`.

It would be ideal for RTL text to appear right-aligned, but alas, see [MC-117311].

### TextHandler’s helper classes

* `WidthRetriever`: A function taking a codepoint and style and returning a width. In Caxton, this is used only when a
  legacy font would be used.
* `WidthLimitingVisitor`: A visitor that stops when the total width of the characters it visits exceeds a given value.
  Obviously assumes that the characters are laid from left to right.
* `LineBreakingVisitor`: Takes a float representing the maximum width.
  When accepting a codepoint, it does the following:
    * If the codepoint is a newline, then starts a new line.
    * If the codepoint is a space, then sets the `lastSpaceBreak` and `lastSpaceStyle` variables. This remembers where
      the last space is and what style it has.
    * Updates the width of the current line.
    * If the line is nonempty and exceeds the maximum width, then starts a new line, breaking at the last visited space
      since the last line break, or if there is none, immediately before the overflowing codepoint.
    * If the codepoint has a nonzero width, then marks the current line as nonempty.
    * Updates the number of code units visited.

  When a new line is started, then the visitor updates the `endIndex` and `endStyle` variables and returns `false`,
  indicating that there are more lines to process. Users of `LineBreakingVisitor` create a new instance for each line
  visited.
* `LineWrappingConsumer`: A function that takes a style plus start and end indices (that would point to a string defined
  elsewhere).
* `LineWrappingCollector`: A helper class that can repeatedly extract a set number of characters from runs of styled
  text. The number of characters to extract is determined by the number of characters that fit before a new line should
  be started.

  Takes a list of styled strings (hereafter ‘parts’), and stores the string resulting from
  joining all of them (i.e. the string without any formatting).
    * The `StringVisitable collectLine(int lineLength, int skippedLength, Style style)` method returns the
      first `lineLength` characters worth of the styled strings, while ignoring the next `skippedLength` characters
      afterwards. It works as such:
        * Create a list of `StringVisitable`s to hold the pending line.
        * Regard the pending line as not yet full.
        * Set the number of remaining characters to `lineLength`.
        * For each of the parts:
            * If the pending line is not yet full, then:
                * If there are enough remaining characters to hold this part and have at least one character left over,
                  then remove this part from the list of parts and add them to the current line. Subtract the number of
                  remaining characters by the length of this part.
                * Otherwise, split the part so that the first part has as many characters as will fit. Add the first
                  part to the current line but do not replace the current part with the second subpart.
                  Add `skippedLength` to the number of remaining characters and regard the pending line as full.
            * If the pending line is now full, then do the following:
                * If there are enough remaining characters to hold this part and have at least one character left over,
                  then remove this part from the list of parts and subtract the number of remaining characters by the
                  length of this part.
                * Otherwise, split the part so that the first part has as many characters as will fit. Replace the
                  current part with the second subpart.
        * Update the unformatted string to reflect the updated list of parts.
        * Return the text of the current line.
    * `StringVisitable collectRemainers()` returns the concatenation of the remaining parts.
* `StyledString` holds a string along with its style; it implements `StringVisitable`.

[MC-117311]: https://bugs.mojang.com/browse/MC-117311

[MC-149453]: https://bugs.mojang.com/browse/MC-149453

## TextFieldWidget

**Unfucking almost complete. Ideally, RTL text should cause the text to be aligned to the right, though.**

:mojank: :concern:

Reinterpret `firstCharacterIndex` as the index of the codepoint that corresponds to the leftmost glyph drawn in the box.

Methods that need to be reimplemented:

* `renderButton`: Add a new method in `CaxtonTextRenderer` to draw a string starting from a given codepoint. (This
  method will have to deal with legacy-font RTL runs, which are currently difficult to support due to
  anemic `ArabicShaping` APIs.)
* `mouseClicked`: Add a method in `CaxtonTextHandler` to get the offset of the `n`th char in the string.
* `setSelectionEnd`: ???

NB: `Util.moveCursor` should perhaps move the cursor by grapheme clusters instead of codepoints

Vanilla Minecraft exhibits the following behavior on `setSelectionEnd`:

* If the cursor has moved to the left of the leftmost displayed character, then the first character index is
  decreased by `N` characters, where `N` is the maximum value such that the last `N` characters of the text fit in the
  allotted width. This is not guaranteed to move the first character index far enough for the cursor to be in range
  again.
* If the cursor was to the right of the rightmost displayed character and has moved right, then the first character
  index is increased by however many characters the cursor exceeds the displayed text by. This is not guaranteed to move
  the first character index far enough for the cursor to be in range again.

## EditBox & EditBoxWidget

**Currently used only for chat reporting, so this has a low priority.**

For `EditBox`, we need to
edit `TextHandler#wrapLines(String text, int maxWidth, Style style, boolean retainTrailingWordSplit, LineWrappingConsumer consumer)`
to allow the text to rewrap properly. `EditBox#moveCursorLine` and `EditBox#moveCursor` also need to be edited to use
proper x-position
calculations.

For `EditBoxWidget`, we need to edit `getRenderContents`.

## BookEditScreen

The following methods need to be fixed:

* For `render`:
    * `drawTrimmed` calls `TextHandler#wrapLines(StringVisitable text2, int maxWidth, Style style)`, which we can fix as
      with all other `wrapLines` methods.
    * `BookEditScreen#drawSelection` and `drawCursor` need to be revised.
        * `drawCursor`: Need to edit `createPageContent`, which is chock-full of broken assumptions about text.
        * `drawSelection`: `getLineSelectionRectangle` is intrinsically broken; replace it with a new method that works
          properly with complex text and outputs multiple rectangles. Fortunately, we can just tell `createPageContent`
          not to use the original method.
* For `mouseClicked` and `mouseDragged`:
    * `PageContent#getCursorPosition` needs no revision, as `TextHandler#getTrimmedLength` does what we want here

Attack strategy:

* Refactor `CaxtonTextHandler#getHighlightRanges` to take a callback instead of returning a list
* Reimplement line-breaking methods in `TextHandler` (hard!)
* Reimplement `BookEditScreen#createPageContent`

### Analysis of createPageContent

* Get the text on the current page.
* If it is empty, then return an empty page content object.
* Let `start` and `end` be the bounds of the current selection.
* Allocate a list of line start indices and a list of lines.
* Set the newline flag to false.
* Line-wrap the page text. For each line:
    * Get the text for that line.
    * Update the newline flag: set it if the line text ends with a newline and reset it otherwise.
    * Strip any trailing newline from the line text.
    * Calculate the position of the line.
    * Add the start index for the line to the list of line starts.
    * Add the line style, text, and position to the list of lines.
* Let `bl` and `bl2` be true if and only if the cursor is at the end of the page.
* Compute the position where the cursor should be drawn.
    * If the cursor is at the end of the page and the newline flag is set, then set the cursor position to be on the
      line
      immediately after the last.
    * Otherwise, compute the y-coordinate from the index of the line in which the cursor lies and the x-coordinate from
      the width of the first `n` characters of the line, where `n` is the number of characters between the start of the
      line and the cursor. (**This needs to be fixed to use `getOffsetAtIndex`.**)
* Compute the positions of the selection rectangles. If the selection contains at least one character, then do the
  following:
    * If `end > start`, then swap `start` and `end` so `start <= end`.
    * Let `startLine` be the index of the line on which char #`start` lies.
    * Let `endLine` be the index of the line on which char #`end` lies.
    * If `startLine` and `endLine` are the same, then add a single rectangle at line #`startLine`.
    * Otherwise:
        * Let `nextLineStart` be the start index of the line after #`startLine`, or the length of the page text
          if `startLine` is the last line.
        * Add a rectangle for the text selected on `startLine`.
        * For each line lying strictly between `startLine` and `endLine`:
            * Add a rectangle for all the text on that line.
        * Add a rectangle for the text selected on `endLine`.
* Return a new `PageContent` object with the computed data.

## How do we wrap text?

The naïve approach: iterate over all possible line breaking points and try to lay out text up to that point. When you
overflow, get the last successful result and make that a line. Repeat until you have no more text.

This, however, is quadratic with respect to the amount of text that fits in the line. Can we do better?

Proposal:

* First, lay out the entire text as a single line.
* Iterate through the `CaxtonText` in logical order, getting the total width of the glyphs.
* Find the last eligible breaking point that does not exceed the allotted width.
* (Optional) If that breaking point splits a run and the shaped text is not unsafe to break at that point, then break
  the shaping results at that point and add them to the cache.
* Collect the text before that point. If it happens to overflow the width in isolation, then use the last break point
  before the one that was used.
* Repeat with the rest of the text (using the existing layout results).
