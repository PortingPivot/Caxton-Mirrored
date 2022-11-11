use std::{
    cmp::Reverse,
    fs::{self, DirBuilder},
    path::Path,
    time::Instant,
};

use anyhow::{anyhow, Context};
use image::{DynamicImage, GenericImage};
use mint::Vector2;
use msdf::{GlyphLoader, Projection, SDFTrait};
use rustybuzz::{Face, GlyphBuffer, UnicodeBuffer};
use sha2::{Digest, Sha256};
use ttf_parser::{GlyphId, Rect};

use crate::atlas::Atlas;

const SALT: [u8; 4] = [0xE6, 0x26, 0x69, 0x11];

/// Margin for stuff.
const MARGIN: u32 = 4;
/// Additional margin to avoid texel bleeding.
const ADDITIONAL_MARGIN: u32 = 0;

/// Font information used to render text by Caxton.
pub struct Font<'a> {
    pub face: Face<'a>,
    pub atlas: Atlas,
    pub bboxes: Vec<u64>,
}

impl<'a> Font<'a> {
    pub fn from_memory(contents: &'a [u8], cache_dir: &'_ Path) -> anyhow::Result<Self> {
        let mut sha = Sha256::new();
        sha.update(contents);
        sha.update(SALT);
        let sha = sha.finalize();

        let mut this_cache = cache_dir.to_path_buf();
        this_cache.push(base64::encode_config(sha, base64::URL_SAFE));

        let face = Face::from_slice(contents, 0).context("failed to parse font")?;

        let bboxes = (0..face.number_of_glyphs())
            .map(|i| {
                let rect = face.glyph_bounding_box(GlyphId(i)).unwrap_or(Rect {
                    x_min: 0,
                    y_min: 0,
                    x_max: 0,
                    y_max: 0,
                });
                (rect.x_min as u16 as u64)
                    | (rect.y_min as u16 as u64) << 16
                    | (rect.x_max as u16 as u64) << 32
                    | (rect.y_max as u16 as u64) << 48
            })
            .collect();

        let atlas = (|| -> anyhow::Result<_> {
            if this_cache.exists() && this_cache.is_dir() {
                match Atlas::load(&this_cache) {
                    Ok(atlas) => return Ok(atlas),
                    Err(e) => {
                        eprintln!("warn: loading atlas failed; regenerating");
                        eprintln!("({e})");
                    }
                }
            }

            eprintln!(
                "Building atlas for {} glyphs; this might take a while",
                face.number_of_glyphs()
            );
            let before_atlas_construction = Instant::now();
            let atlas = create_atlas(&face)?;
            let after_atlas_construction = Instant::now();
            eprintln!(
                "Built atlas in {} ms! ^_^",
                (after_atlas_construction - before_atlas_construction).as_millis()
            );
            if this_cache.exists() {
                fs::remove_file(&this_cache)
                    .or_else(|_| fs::remove_dir_all(&this_cache))
                    .context("failed to remove old cache data")?;
            }
            DirBuilder::new().recursive(true).create(&this_cache)?;
            eprintln!("saving atlas to {}...", this_cache.display());
            if let Err(e) = atlas.save(&this_cache) {
                eprintln!("warn: failed to save cached atlas: {e}")
            }
            Ok(atlas)
        })()?;

        Ok(Font {
            face,
            atlas,
            bboxes,
        })
    }

    pub fn shape(&self, buffer: UnicodeBuffer) -> GlyphBuffer {
        // TODO: provide way to configure features in font
        rustybuzz::shape(&self.face, &[], buffer)
    }
}

fn create_atlas(face: &Face) -> anyhow::Result<Atlas> {
    let msdf_config = Default::default();

    let mut atlas = Atlas::builder();

    let mut glyph_indices = (0..face.number_of_glyphs())
        .map(|glyph_idx| (glyph_idx, face.glyph_bounding_box(GlyphId(glyph_idx))))
        .collect::<Vec<_>>();
    glyph_indices.sort_by_key(|(_, bb)| bb.map(|bb| Reverse(bb.height())));

    for (glyph_index, bounding_box) in glyph_indices {
        let glyph_id = GlyphId(glyph_index);

        let bounding_box = match bounding_box {
            Some(s) => s,
            None => {
                eprintln!("glyph #{glyph_index} has no bounding box; skipping");
                atlas.insert(glyph_id, 0, 0)?;
                continue;
            }
        };

        let width = (bounding_box.width().unsigned_abs() as u32).div_ceil(64) + 2 * MARGIN;
        let height = (bounding_box.height().unsigned_abs() as u32).div_ceil(64) + 2 * MARGIN;
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
                y: -(MARGIN as f64),
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
