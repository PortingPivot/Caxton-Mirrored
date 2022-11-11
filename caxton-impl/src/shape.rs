use std::slice;

use rustybuzz::GlyphBuffer;

#[repr(transparent)]
pub struct ShapingResultEntry(pub [i32; 6]);

pub struct ShapingResult {
    pub data: Vec<ShapingResultEntry>,
    pub total_width: i32,
}

impl ShapingResult {
    pub fn from_glyph_buffer(buffer: &GlyphBuffer) -> Self {
        let mut data = Vec::with_capacity(buffer.len());
        let mut total_width = 0;

        let glyph_infos = buffer.glyph_infos();
        let glyph_positions = buffer.glyph_positions();

        for i in 0..buffer.len() {
            let info = &glyph_infos[i];
            let position = &glyph_positions[i];

            data.push(ShapingResultEntry([
                (info.glyph_id as i32) | (info.unsafe_to_break() as i32) << 16,
                info.cluster as i32,
                position.x_advance,
                position.y_advance,
                position.x_offset,
                position.y_offset,
            ]));
            total_width += position.x_advance;
        }

        ShapingResult { data, total_width }
    }

    pub fn data_as_i32s(&self) -> &[i32] {
        let slice = &self.data[..];
        // SAFETY: slice has the same layout as [[i32; 6]], which we can transmute to [i32]
        unsafe { slice::from_raw_parts(slice.as_ptr().cast::<i32>(), 6 * slice.len()) }
    }
}
