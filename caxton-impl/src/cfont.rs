use std::{slice, str::FromStr};

use rustybuzz::{Feature, GlyphBuffer, UnicodeBuffer};
use serde::{de, Deserialize};

use crate::font::Font;

#[repr(transparent)]
#[derive(Copy, Clone, Debug)]
pub struct CxFeature(pub Feature);

impl<'de> Deserialize<'de> for CxFeature {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let feature_string = <&str>::deserialize(deserializer)?;
        let feature = Feature::from_str(feature_string).map_err(|_e| {
            de::Error::invalid_value(de::Unexpected::Str(feature_string), &"a feature string")
        })?;
        Ok(CxFeature(feature))
    }
}

#[derive(Deserialize)]
#[serde(default)]
pub struct ConfiguredFontSettings {
    pub features: Vec<CxFeature>,
}

impl Default for ConfiguredFontSettings {
    fn default() -> Self {
        Self {
            features: Default::default(),
        }
    }
}

pub struct ConfiguredFont<'font> {
    pub font: &'font Font<'font>,
    pub settings: ConfiguredFontSettings,
}

impl<'font> ConfiguredFont<'font> {
    pub fn shape(&self, buffer: UnicodeBuffer) -> GlyphBuffer {
        // TODO: provide way to configure features in font
        let features = &self.settings.features;
        // SAFETY: CxFeature and Feature are layout-compatible
        let features = unsafe { slice::from_raw_parts(features.as_ptr().cast(), features.len()) };
        rustybuzz::shape(&self.font.face, features, buffer)
    }
}
