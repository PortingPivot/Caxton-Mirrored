use std::{
    cmp::Reverse,
    fs::{self, DirBuilder},
    path::Path,
    time::Instant,
};

use anyhow::{anyhow, Context};
use image::{buffer::ConvertBuffer, GenericImage, RgbaImage};
use mint::Vector2;
use msdf::{GlyphLoader, Projection, SDFTrait};
use rustybuzz::{Face, GlyphBuffer, Tag, UnicodeBuffer};
use serde::{de, Deserialize};
use sha2::{Digest, Sha256};
use ttf_parser::{GlyphId, Rect, Variation};

use crate::atlas::Atlas;

const SALT: [u8; 4] = [0xE6, 0x26, 0x69, 0x11];

#[derive(Deserialize)]
struct CxVariationFormat<'a> {
    axis: &'a str,
    value: f32,
}

#[repr(transparent)]
#[derive(Debug, Copy, Clone)]
pub struct CxVariation(pub Variation);

impl<'de> Deserialize<'de> for CxVariation {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let format = CxVariationFormat::deserialize(deserializer)?;
        let axis = if format.axis.len() == 4 {
            Tag::from_bytes_lossy(format.axis.as_bytes())
        } else {
            return Err(de::Error::invalid_value(
                de::Unexpected::Str(&format.axis),
                &"a string of 4 characters",
            ));
        };
        Ok(CxVariation(Variation {
            axis,
            value: format.value,
        }))
    }
}

#[derive(Deserialize, Debug)]
#[serde(default)]
pub struct FontOptions {
    pub shrinkage: f64,
    pub margin: u32,
    pub range: u32,
    pub invert: bool,
    pub page_size: u32,
    pub variations: Vec<CxVariation>,
}

impl Default for FontOptions {
    fn default() -> Self {
        Self {
            shrinkage: 32.0,
            margin: 8,
            range: 4,
            invert: false,
            page_size: 4096,
            variations: Default::default(),
        }
    }
}

/// Font information used to render text by Caxton.
pub struct Font<'a> {
    pub face: Face<'a>,
    pub atlas: Atlas,
    pub bboxes: Vec<u64>,
}

impl<'a> Font<'a> {
    pub fn from_memory(
        contents: &'a [u8],
        cache_dir: &'_ Path,
        options: &FontOptions,
    ) -> anyhow::Result<Self> {
        let mut sha = Sha256::new();
        sha.update(contents);
        sha.update(&format!(
            "{} {} {} {} {}",
            options.shrinkage, options.margin, options.range, options.invert, options.page_size,
        ));
        for var in &options.variations {
            sha.update(&format!(" {} {}", var.0.axis, var.0.value));
        }
        sha.update(SALT);
        let sha = sha.finalize();

        let mut this_cache = cache_dir.to_path_buf();
        this_cache.push(base64::encode_config(sha, base64::URL_SAFE));

        let mut face = Face::from_slice(contents, 0).context("failed to parse font")?;
        for var in &options.variations {
            face.set_variation(var.0.axis, var.0.value)
                .ok_or_else(|| anyhow!("axis {} not found", var.0.axis))?;
        }

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
                match Atlas::load(&this_cache, options.page_size) {
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
            let atlas = create_atlas(&face, options)?;
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

fn create_atlas(face: &Face, options: &FontOptions) -> anyhow::Result<Atlas> {
    let msdf_config = Default::default();

    let mut atlas = Atlas::builder(options.page_size);

    let mut glyph_indices = (0..face.number_of_glyphs())
        .map(|glyph_idx| (glyph_idx, face.glyph_bounding_box(GlyphId(glyph_idx))))
        .collect::<Vec<_>>();
    glyph_indices.sort_by_key(|(_, bb)| bb.map(|bb| Reverse(bb.height())));

    for (glyph_index, bounding_box) in glyph_indices {
        let glyph_id = GlyphId(glyph_index);

        let bounding_box = match bounding_box {
            Some(s) => s,
            None => {
                atlas.insert(glyph_id, 0, 0, 1)?;
                continue;
            }
        };

        let width = (bounding_box.width().unsigned_abs() as f64 / options.shrinkage).ceil() as u32
            + 2 * options.margin;
        let height = (bounding_box.height().unsigned_abs() as f64 / options.shrinkage).ceil()
            as u32
            + 2 * options.margin;
        let location = atlas.insert(glyph_id, width, height, 1)?;

        let shape = face
            .as_ref()
            .load_shape(glyph_id)
            .ok_or_else(|| anyhow!("could not load glyph #{glyph_index}"))?;
        let colored_shape = shape.color_edges_simple(3.0);
        let projection = Projection {
            scale: Vector2 {
                x: 1.0 / options.shrinkage,
                y: 1.0 / options.shrinkage,
            },
            translation: Vector2 {
                x: options.margin as f64 * options.shrinkage - bounding_box.x_min as f64,
                y: options.margin as f64 * options.shrinkage - bounding_box.y_min as f64,
            },
        };

        let mtsdf = colored_shape.generate_mtsdf(
            width,
            height,
            options.range as f64 * options.shrinkage,
            &projection,
            &msdf_config,
        );

        let page = atlas
            .page_mut(location.page_index() as usize)
            .context("failed to get page – this is a bug")?;

        let mut image: RgbaImage = mtsdf.to_image().convert();

        if options.invert {
            for pixel in image.pixels_mut() {
                for value in &mut pixel.0 {
                    *value = !*value;
                }
            }
        }

        page.copy_from(&image, location.x(), location.y())?;
    }

    Ok(atlas.build())
}
