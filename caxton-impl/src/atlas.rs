use std::{
    collections::HashMap,
    fmt::{self, Debug, Formatter},
};

use anyhow::bail;
use image::RgbaImage;
use ttf_parser::GlyphId;

pub const MAX_SIZE: u32 = 4096;

/// A location in an atlas.
///
/// This consists of the position and size of the rectangle,
/// as well as the index of the page that it describes.
#[derive(Copy, Clone, PartialEq, Eq)]
#[repr(transparent)]
pub struct Location {
    packed: u64,
}

impl Location {
    pub fn new(x: u32, y: u32, width: u32, height: u32, page: u32) -> Self {
        assert!(x < 2 * MAX_SIZE, "x must be in [0, 8192) (is {x})");
        assert!(y < 2 * MAX_SIZE, "y must be in [0, 8192) (is {y})");
        assert!(
            width < 2 * MAX_SIZE,
            "width must be in [0, 8192) (is {width})"
        );
        assert!(
            height < 2 * MAX_SIZE,
            "height must be in [0, 8192) (is {height})"
        );
        assert!(page < 4096, "page must be in [0, 4096) (is {page})");
        Self {
            packed: (x as u64)
                | ((y as u64) << 13)
                | ((width as u64) << 26)
                | ((height as u64) << 39)
                | ((page as u64) << 52),
        }
    }

    pub fn x(self) -> u32 {
        (self.packed & 0x1FFF) as u32
    }

    pub fn y(self) -> u32 {
        ((self.packed >> 13) & 0x1FFF) as u32
    }

    pub fn width(self) -> u32 {
        ((self.packed >> 26) & 0x1FFF) as u32
    }

    pub fn height(self) -> u32 {
        ((self.packed >> 39) & 0x1FFF) as u32
    }

    pub fn page_index(self) -> u32 {
        (self.packed >> 52) as u32
    }
}

impl Debug for Location {
    fn fmt(&self, f: &mut Formatter<'_>) -> fmt::Result {
        f.debug_struct("Location")
            .field("x", &self.x())
            .field("y", &self.y())
            .field("width", &self.width())
            .field("height", &self.height())
            .field("page_index", &self.page_index())
            .finish()
    }
}

/// An immutable atlas of glyph IDs.
#[derive(Debug, Clone)]
pub struct Atlas {
    pages: Vec<RgbaImage>,
    locations: HashMap<GlyphId, Location>,
}

impl Atlas {
    pub fn builder() -> AtlasBuilder {
        AtlasBuilder {
            atlas: Atlas {
                pages: Vec::new(),
                locations: HashMap::new(),
            },
            spaces: Vec::new(),
        }
    }

    pub fn glyph_location(&self, glyph_id: GlyphId) -> Option<Location> {
        self.locations.get(&glyph_id).copied()
    }

    pub fn page(&self, index: usize) -> Option<&RgbaImage> {
        self.pages.get(index)
    }
}

/// Supports building glyph atlases.
pub struct AtlasBuilder {
    atlas: Atlas,
    spaces: Vec<Location>,
}

impl AtlasBuilder {
    pub fn build(self) -> Atlas {
        self.atlas
    }

    pub fn insert(
        &mut self,
        glyph_id: GlyphId,
        width: u32,
        height: u32,
    ) -> anyhow::Result<Location> {
        if width >= MAX_SIZE || height >= MAX_SIZE {
            bail!("Image to be inserted is too big (max: 4096x4096; given: {width}x{height})");
        }
        if self.atlas.locations.contains_key(&glyph_id) {
            bail!(
                "Cannot insert the same glyph ID twice (#{} already exists)",
                glyph_id.0
            );
        }
        let space = loop {
            match self.try_insert(width, height) {
                Some(space) => break space,
                None => self.add_new_page(),
            }
        };
        self.atlas.locations.insert(glyph_id, space);
        Ok(space)
    }

    fn try_insert(&mut self, width: u32, height: u32) -> Option<Location> {
        for i in (0..self.spaces.len()).rev() {
            let space = self.spaces[i];
            if space.width() < width || space.height() < height {
                continue;
            }
            self.spaces.swap_remove(i);
            if space.height() > height {
                self.spaces.push(Location::new(
                    space.x(),
                    space.y() + height,
                    space.width(),
                    space.height() - height,
                    space.page_index(),
                ));
            }
            if space.width() > width {
                self.spaces.push(Location::new(
                    space.x() + width,
                    space.y(),
                    space.width() - width,
                    space.height(),
                    space.page_index(),
                ));
            }
            return Some(Location::new(
                space.x(),
                space.y(),
                width,
                height,
                space.page_index(),
            ));
        }
        None
    }

    fn add_new_page(&mut self) {
        let index = self.atlas.pages.len();
        self.atlas.pages.push(RgbaImage::new(MAX_SIZE, MAX_SIZE));
        self.spaces
            .push(Location::new(0, 0, MAX_SIZE, MAX_SIZE, index as u32));
    }

    pub fn page_mut(&mut self, index: usize) -> Option<&mut RgbaImage> {
        self.atlas.pages.get_mut(index)
    }
}
