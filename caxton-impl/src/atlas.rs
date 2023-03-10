use std::{
    fmt::{self, Debug, Formatter},
    fs::OpenOptions,
    path::Path,
};

use anyhow::{bail, Context};
use image::RgbaImage;
use io_ext::{ReadExt, WriteExt};
use ttf_parser::GlyphId;

/// A location in an atlas.
///
/// This consists of the position and size of the rectangle,
/// as well as the index of the page that it describes.
#[derive(Copy, Clone, PartialEq, Eq)]
#[repr(transparent)]
pub struct Location {
    packed: u64,
}

pub const INVALID: Location = Location { packed: u64::MAX };

impl Location {
    pub fn new(x: u32, y: u32, width: u32, height: u32, page: u32) -> Self {
        assert!(x < 8192, "x must be in [0, 8192) (is {x})");
        assert!(y < 8192, "y must be in [0, 8192) (is {y})");
        assert!(width < 8192, "width must be in [0, 8192) (is {width})");
        assert!(height < 8192, "height must be in [0, 8192) (is {height})");
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
    locations: Vec<Location>,
}

const VERSION: u32 = 1;

impl Atlas {
    pub fn builder(page_size: u32) -> AtlasBuilder {
        AtlasBuilder {
            atlas: Atlas {
                pages: Vec::new(),
                locations: Vec::new(),
            },
            spaces: Vec::new(),
            page_size,
        }
    }

    pub fn glyph_location(&self, glyph_id: GlyphId) -> Option<Location> {
        self.locations.get(glyph_id.0 as usize).copied()
    }

    pub fn glyph_locations(&self) -> &[Location] {
        &self.locations
    }

    pub(crate) fn num_pages(&self) -> usize {
        self.pages.len()
    }

    pub fn page(&self, index: usize) -> Option<&RgbaImage> {
        self.pages.get(index)
    }

    pub fn save(&self, dir: &'_ Path) -> anyhow::Result<()> {
        {
            let mut locations_file = dir.to_path_buf();
            locations_file.push("locations.bin");
            let mut fh = OpenOptions::new()
                .write(true)
                .create(true)
                .open(locations_file)
                .context("failed to write to locations.bin")?;
            fh.write_u32_checked(VERSION)?;
            fh.write_u16_checked(self.locations.len().try_into()?)?;
            fh.write_u16_checked(self.pages.len().try_into()?)?;
            for l in &self.locations {
                fh.write_u64_checked(l.packed)?;
            }
        }

        for (i, page) in self.pages.iter().enumerate() {
            let mut img = dir.to_path_buf();
            img.push(format!("atlas{i}.png"));
            page.save(img).context("failed to save image")?;
        }

        Ok(())
    }

    pub fn load(dir: &'_ Path, expected_page_size: u32) -> anyhow::Result<Self> {
        let mut locations_file = dir.to_path_buf();
        locations_file.push("locations.bin");
        let mut fh = OpenOptions::new()
            .read(true)
            .open(locations_file)
            .context("failed to read locations.bin")?;

        let version = fh.read_u32_checked()?;
        if version != VERSION {
            bail!("version mismatch (expected version {VERSION}; found {version}");
        }

        let num_locations = fh.read_u16_checked()?;
        let num_pages = fh.read_u16_checked()?;

        let mut locations = Vec::with_capacity(num_locations as usize);
        for _ in 0..num_locations {
            let location = Location {
                packed: fh.read_u64_checked()?,
            };
            locations.push(location);
        }

        let mut pages = Vec::with_capacity(num_pages as usize);
        for i in 0..num_pages {
            let mut img_path = dir.to_path_buf();
            img_path.push(format!("atlas{i}.png"));
            let page = image::io::Reader::open(img_path)
                .context("failed to open image")?
                .decode()
                .context("failed to decode image")?
                .into_rgba8();
            if page.width() != expected_page_size || page.height() != expected_page_size {
                bail!("page #{i} has wrong size (expected {expected_page_size}x{expected_page_size}; got {}x{})", page.width(), page.height());
            }
            pages.push(page);
        }

        Ok(Self { pages, locations })
    }
}

/// Supports building glyph atlases.
pub struct AtlasBuilder {
    atlas: Atlas,
    spaces: Vec<Location>,
    page_size: u32,
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
        additional_margin: u32,
    ) -> anyhow::Result<Location> {
        let total_width = width + additional_margin;
        let total_height = height + additional_margin;
        if total_width >= self.page_size || total_height >= self.page_size {
            bail!(
                "Image to be inserted is too big (max: {}x{}); given: {width}x{height})",
                self.page_size,
                self.page_size
            );
        }
        if let Some(s) = self.atlas.glyph_location(glyph_id) {
            if s != INVALID {
                bail!(
                    "Cannot insert the same glyph ID twice (#{} already exists)",
                    glyph_id.0
                );
            }
        }
        if width == 0 && height == 0 {
            return Ok(Location { packed: 0 });
        }
        let space = loop {
            match self.try_insert(total_width, total_height) {
                Some(space) => break space,
                None => self.add_new_page(),
            }
        };
        if glyph_id.0 as usize + 1 > self.atlas.locations.len() {
            self.atlas
                .locations
                .resize(glyph_id.0 as usize + 1, INVALID);
        }
        self.atlas.locations[glyph_id.0 as usize] = space;
        Ok(Location::new(
            space.x(),
            space.y(),
            space.width() - additional_margin,
            space.height() - additional_margin,
            space.page_index(),
        ))
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
                    height,
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
        self.atlas
            .pages
            .push(RgbaImage::new(self.page_size, self.page_size));
        self.spaces.push(Location::new(
            0,
            0,
            self.page_size,
            self.page_size,
            index as u32,
        ));
    }

    pub fn page_mut(&mut self, index: usize) -> Option<&mut RgbaImage> {
        self.atlas.pages.get_mut(index)
    }
}
