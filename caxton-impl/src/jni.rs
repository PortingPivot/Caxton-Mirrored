use std::{mem, ops::Deref, path::PathBuf, ptr, slice};

use anyhow::Context;
use jni::{
    objects::{JByteBuffer, JClass, JObject, JString, JValue, ReleaseMode},
    sys::{jcharArray, jint, jintArray, jlong, jobjectArray, jshortArray},
    JNIEnv,
};
use rustybuzz::UnicodeBuffer;
use thiserror::Error;

use crate::{font::Font, shape::ShapingResult};

#[derive(Error, Debug)]
pub enum CxtError {
    #[error("{0}")]
    Mine(#[from] anyhow::Error),
    #[error("JNI error: {0:?}")]
    Jni(#[from] jni::errors::Error),
}

macro_rules! throw_as_exn {
    ($env:ident, $default:expr; $($tt:tt)*) => {
        match (|| -> Result<_, CxtError> {
            $($tt)*
        })() {
            Ok(value) => value,
            Err(CxtError::Mine(err)) => {
                let _ = $env.throw_new("java/lang/RuntimeException", err.to_string());
                $default
            }
            Err(CxtError::Jni(err)) => {
                let _ = $env.throw_new("java/lang/RuntimeException", err.to_string());
                $default
            }
        }
    };
    ($env:ident; $($tt:tt)*) => {
        throw_as_exn! {
            $env, Default::default();
            $($tt)*
        }
    };
}

/// JNI wrapper around [`Font::from_memory`].
///
/// # Safety
///
/// `font_data` must be a direct byte buffer.
// public static native long createFont(ByteBuffer fontData, String cachePath);
#[no_mangle]
pub unsafe extern "system" fn Java_xyz_flirora_caxton_font_CaxtonInternal_createFont(
    env: JNIEnv,
    _class: JClass,
    font_data: JByteBuffer,
    cache_path: JString,
) -> jlong {
    throw_as_exn! {
        env;
        let cache_path: String = env
            .get_string(cache_path)?
            .into();
        let font_data = slice::from_raw_parts(
            env.get_direct_buffer_address(font_data)?,
            env.get_direct_buffer_capacity(font_data)?,
        );
        let font = Box::new(
            Font::from_memory(font_data, &PathBuf::from(cache_path)).context("font creation failed")?,
        );
        Ok(Box::into_raw(font) as usize as jlong)
    }
}

/// JNI wrapper for dropping a [`Font`].
///
/// # Safety
///
/// `addr` must have previously been returned by [`Java_xyz_flirora_caxton_font_CaxtonInternal_createFont`]
/// and must not have been previously passed into [`Java_xyz_flirora_caxton_font_CaxtonInternal_destroyFont`].
// public static native void destroyFont(long addr);
#[no_mangle]
pub unsafe extern "system" fn Java_xyz_flirora_caxton_font_CaxtonInternal_destroyFont(
    _env: JNIEnv,
    _class: JClass,
    addr: jlong,
) {
    if addr == 0 {
        eprintln!("warn: was passed an address of 0; returning");
        return;
    }
    mem::drop(Box::from_raw(addr as usize as *mut Font));
}

/// JNI wrapper for [`rustybuzz::Face::glyph_index`].
///
/// # Safety
///
/// `addr` must have previously been returned by [`Java_xyz_flirora_caxton_font_CaxtonInternal_createFont`]
/// and must not have been previously passed into [`Java_xyz_flirora_caxton_font_CaxtonInternal_destroyFont`].
// public static native int fontGlyphIndex(long addr, int codePoint);
#[no_mangle]
pub unsafe extern "system" fn Java_xyz_flirora_caxton_font_CaxtonInternal_fontGlyphIndex(
    _env: JNIEnv,
    _class: JClass,
    addr: jlong,
    codepoint: jint,
) -> jint {
    if addr == 0 {
        eprintln!("warn: was passed an address of 0; returning");
        return -1;
    }
    let codepoint = match char::try_from(codepoint as u32) {
        Ok(codepoint) => codepoint,
        Err(_) => return -1,
    };
    (*(addr as usize as *const Font))
        .face
        .glyph_index(codepoint)
        .map(|x| x.0 as i32)
        .unwrap_or(-1)
}

/// JNI wrapper for various [`rustybuzz::Face`] methods.
///
/// # Safety
///
/// `addr` must have previously been returned by [`Java_xyz_flirora_caxton_font_CaxtonInternal_createFont`]
/// and must not have been previously passed into [`Java_xyz_flirora_caxton_font_CaxtonInternal_destroyFont`].
// public static native short[] fontMetrics(long addr);
#[no_mangle]
pub unsafe extern "system" fn Java_xyz_flirora_caxton_font_CaxtonInternal_fontMetrics(
    env: JNIEnv,
    _class: JClass,
    addr: jlong,
) -> jshortArray {
    throw_as_exn! {
        env, ptr::null_mut();
        if addr == 0 {
            eprintln!("warn: was passed an address of 0; returning");
            return Ok(ptr::null_mut());
        }
        let font = (*(addr as usize as *const Font)).face.as_ref();
        let (underline_position, underline_thickness) = match font.underline_metrics() {
            Some(underline) => (underline.position, underline.thickness),
            None => (-1, -1),
        };
        let metrics = [
            font.units_per_em() as i16,
            font.ascender(),
            font.descender(),
            font.height(),
            font.line_gap(),
            underline_position,
            underline_thickness,
        ];
        let output = env.new_short_array(metrics.len() as i32)?;
        env.set_short_array_region(output, 0, &metrics)?;
        Ok(output)
    }
}

/// JNI wrapper for the number of atlas locations.
///
/// # Safety
///
/// `addr` must have previously been returned by [`Java_xyz_flirora_caxton_font_CaxtonInternal_createFont`]
/// and must not have been previously passed into [`Java_xyz_flirora_caxton_font_CaxtonInternal_destroyFont`].
// public static native int fontAtlasSize(long addr);
#[no_mangle]
pub unsafe extern "system" fn Java_xyz_flirora_caxton_font_CaxtonInternal_fontAtlasSize(
    _env: JNIEnv,
    _class: JClass,
    addr: jlong,
) -> jint {
    if addr == 0 {
        eprintln!("warn: was passed an address of 0; returning");
        return 0;
    }
    (*(addr as usize as *const Font))
        .atlas
        .glyph_locations()
        .len() as i32
}

/// JNI wrapper for accessing atlas locations.
///
/// # Safety
///
/// `addr` must have previously been returned by [`Java_xyz_flirora_caxton_font_CaxtonInternal_createFont`]
/// and must not have been previously passed into [`Java_xyz_flirora_caxton_font_CaxtonInternal_destroyFont`].
// public static native long fontAtlasLocations(long addr);
#[no_mangle]
pub unsafe extern "system" fn Java_xyz_flirora_caxton_font_CaxtonInternal_fontAtlasLocations(
    _env: JNIEnv,
    _class: JClass,
    addr: jlong,
) -> jlong {
    if addr == 0 {
        eprintln!("warn: was passed an address of 0; returning");
        return 0;
    }
    (*(addr as usize as *const Font))
        .atlas
        .glyph_locations()
        .as_ptr() as usize as i64
}

/// JNI wrapper for accessing bounding box data.
///
/// # Safety
///
/// `addr` must have previously been returned by [`Java_xyz_flirora_caxton_font_CaxtonInternal_createFont`]
/// and must not have been previously passed into [`Java_xyz_flirora_caxton_font_CaxtonInternal_destroyFont`].
// public static native long fontBboxes(long addr);
#[no_mangle]
pub unsafe extern "system" fn Java_xyz_flirora_caxton_font_CaxtonInternal_fontBboxes(
    _env: JNIEnv,
    _class: JClass,
    addr: jlong,
) -> jlong {
    if addr == 0 {
        eprintln!("warn: was passed an address of 0; returning");
        return 0;
    }
    (*(addr as usize as *const Font)).bboxes.as_ptr() as usize as i64
}

/// JNI wrapper for accessing the number of atlas pages.
///
/// # Safety
///
/// `addr` must have previously been returned by [`Java_xyz_flirora_caxton_font_CaxtonInternal_createFont`]
/// and must not have been previously passed into [`Java_xyz_flirora_caxton_font_CaxtonInternal_destroyFont`].
// public static native int fontAtlasNumPages(long addr);
#[no_mangle]
pub unsafe extern "system" fn Java_xyz_flirora_caxton_font_CaxtonInternal_fontAtlasNumPages(
    _env: JNIEnv,
    _class: JClass,
    addr: jlong,
) -> jint {
    if addr == 0 {
        eprintln!("warn: was passed an address of 0; returning");
        return 0;
    }
    (*(addr as usize as *const Font)).atlas.num_pages() as i32
}

/// JNI wrapper for accessing the number of atlas pages.
///
/// # Safety
///
/// `addr` must have previously been returned by [`Java_xyz_flirora_caxton_font_CaxtonInternal_createFont`]
/// and must not have been previously passed into [`Java_xyz_flirora_caxton_font_CaxtonInternal_destroyFont`].
// public static native int fontAtlasPage(long addr, int pageNum);
#[no_mangle]
pub unsafe extern "system" fn Java_xyz_flirora_caxton_font_CaxtonInternal_fontAtlasPage(
    env: JNIEnv,
    _class: JClass,
    addr: jlong,
    page_num: jint,
) -> jlong {
    if addr == 0 {
        eprintln!("warn: was passed an address of 0; returning");
        return 0;
    }
    throw_as_exn! {
        env;
        let page = (*(addr as usize as *const Font)).atlas.page(page_num as usize).context("page out of bounds")?;
        let ptr: *const u8 = page.deref().as_ptr();
        Ok(ptr as i64)
    }
}

/// Shapes a number of runs over a string.
///
/// # Safety
///
/// `font_addr` must have previously been returned by [`Java_xyz_flirora_caxton_font_CaxtonInternal_createFont`]
/// and must not have been previously passed into [`Java_xyz_flirora_caxton_font_CaxtonInternal_destroyFont`].
///
/// `bidi_runs` must have a length divisible by 2.
// public static native ShapingResult[] shape(long fontAddr, char[] s, int[] bidiRuns);
#[no_mangle]
pub unsafe extern "system" fn Java_xyz_flirora_caxton_font_CaxtonInternal_shape(
    env: JNIEnv,
    _class: JClass,
    font_addr: jlong,
    s: jcharArray,
    bidi_runs: jintArray,
) -> jobjectArray {
    throw_as_exn! {
        env, ptr::null_mut();
        let shaping_result_class = env
            .find_class("xyz/flirora/caxton/font/ShapingResult")?;
        let shaping_result_ctor = env
            .get_method_id("xyz/flirora/caxton/font/ShapingResult", "<init>", "([II)V")?;

        let string = env
            .get_char_array_elements(s, ReleaseMode::NoCopyBack)?;
        let bidi_runs = env
            .get_int_array_elements(bidi_runs, ReleaseMode::NoCopyBack)?;
        let string = slice::from_raw_parts(string.as_ptr(), string.size()? as usize);
        let bidi_runs = slice::from_raw_parts(bidi_runs.as_ptr(), bidi_runs.size()? as usize);
        let font = &*(font_addr as usize as *const Font);

        let num_bidi_runs = bidi_runs.len() / 2;
        let output = env
            .new_object_array(num_bidi_runs as i32, shaping_result_class, JObject::null())?;

        let mut buffer = UnicodeBuffer::new();

        for i in 0..num_bidi_runs {
            let start = bidi_runs[2 * i] as usize;
            let end = bidi_runs[2 * i + 1] as usize;
            let substring = &string[start..end];
            // This is not ideal – rustybuzz only exposes a UTF-8
            // `push_str` method for `UnicodeBuffer` and doesn’t expose
            // any way to set context codepoints.
            let substring = String::from_utf16_lossy(substring);
            buffer.push_str(&substring);

            let shaped = font.shape(mem::take(&mut buffer));
            let sr = ShapingResult::from_glyph_buffer(&shaped);

            let data = env.new_int_array(6 * sr.data.len() as i32)?;
            env.set_int_array_region(data, 0, sr.data_as_i32s())?;

            let object = env
                .new_object_unchecked(
                    shaping_result_class,
                    shaping_result_ctor,
                    &[
                        JValue::Object(JObject::from_raw(data)),
                        JValue::Int(sr.total_width),
                    ],
                )
                ?;

            env.set_object_array_element(output, i as i32, object)?;

            buffer = shaped.clear();
        }

        Ok(output)
    }
}
