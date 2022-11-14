use rustybuzz::{GlyphBuffer, UnicodeBuffer};
use serde::Deserialize;

use crate::font::Font;

#[derive(Deserialize)]
#[serde(default)]
pub struct ConfiguredFontSettings {
    //
}

impl Default for ConfiguredFontSettings {
    fn default() -> Self {
        Self {}
    }
}

pub struct ConfiguredFont<'font> {
    pub font: &'font Font<'font>,
    pub settings: ConfiguredFontSettings,
}

impl<'font> ConfiguredFont<'font> {
    pub fn shape(&self, buffer: UnicodeBuffer) -> GlyphBuffer {
        // TODO: provide way to configure features in font
        rustybuzz::shape(&self.font.face, &[], buffer)
    }
}
