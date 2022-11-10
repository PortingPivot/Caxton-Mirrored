use std::path::Path;

use anyhow::{anyhow, Context};
use image::{DynamicImage, GenericImage};
use mint::Vector2;
use msdf::{GlyphLoader, Projection, SDFTrait};
use rustybuzz::Face;
use sha2::{Digest, Sha256};
use ttf_parser::GlyphId;

use crate::atlas::Atlas;

const SALT: [u8; 4] = [0xE6, 0x26, 0x69, 0x11];

/// Margin for stuff.
const MARGIN: u32 = 4;
/// Additional margin to avoid texel bleeding.
const ADDITIONAL_MARGIN: u32 = 0;

pub struct Font<'a> {
    face: Face<'a>,
    atlas: Atlas,
}

impl<'a> Font<'a> {
    pub fn from_memory(contents: &'a [u8], cache_dir: &'_ Path) -> anyhow::Result<Self> {
        let mut sha = Sha256::new();
        sha.update(contents);
        sha.update(SALT);
        let sha = sha.finalize();
        let cache_folder = base64::encode(sha);
        eprintln!("cache folder: {cache_folder} (not yet used)");

        let face = Face::from_slice(contents, 0).context("failed to parse font")?;
        let atlas = create_atlas(&face)?;

        Ok(Font { face, atlas })
    }
}

fn create_atlas(face: &Face) -> anyhow::Result<Atlas> {
    let msdf_config = Default::default();

    let mut atlas = Atlas::builder();

    for glyph_index in 0..face.number_of_glyphs() {
        let glyph_id = GlyphId(glyph_index);

        let bounding_box = face
            .glyph_bounding_box(glyph_id)
            .ok_or_else(|| anyhow!("failed to get bounding box of glyph #{glyph_index}"))?;

        eprintln!("bounding box: {bounding_box:?}");

        let width = bounding_box.width().unsigned_abs() as u32 + 2 * MARGIN;
        let height = bounding_box.height().unsigned_abs() as u32 + 2 * MARGIN;
        let location = atlas.insert(glyph_id, width, height)?;

        let shape = face
            .as_ref()
            .load_shape(glyph_id)
            .ok_or_else(|| anyhow!("could not load glyph #{glyph_index}"))?;
        let colored_shape = shape.color_edges_simple(3.0);
        let projection = Projection {
            scale: Vector2 {
                x: 1.0 / 64.0,
                y: 1.0 / 64.0,
            },
            translation: Vector2 {
                x: MARGIN as f64,
                y: MARGIN as f64,
            },
        };
        let mtsdf =
            colored_shape.generate_mtsdf(width, height, MARGIN as f64, &projection, &msdf_config);

        let page = atlas
            .page_mut(location.page_index() as usize)
            .context("failed to get page – this is a bug")?;

        let image = DynamicImage::ImageRgba32F(mtsdf.to_image());

        page.copy_from(&image, location.x(), location.y())?;
    }

    Ok(atlas.build())
}
