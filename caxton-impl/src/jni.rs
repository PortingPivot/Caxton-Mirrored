use std::{mem, path::PathBuf, slice};

use jni::{
    objects::{JByteBuffer, JClass, JObject, JString, JValue, ReleaseMode},
    sys::{jcharArray, jint, jintArray, jlong, jobjectArray},
    JNIEnv,
};
use rustybuzz::UnicodeBuffer;

use crate::{font::Font, shape::ShapingResult};

// TODO: replace unwraps and expects with raising Java exceptions

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
    let cache_path: String = env
        .get_string(cache_path)
        .expect("couldn't get Java string")
        .into();
    let font_data = slice::from_raw_parts(
        env.get_direct_buffer_address(font_data).unwrap(),
        env.get_direct_buffer_capacity(font_data).unwrap(),
    );
    let font = Box::new(
        Font::from_memory(font_data, &PathBuf::from(cache_path)).expect("font creation failed"),
    );
    Box::into_raw(font) as u64 as jlong
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
    mem::drop(Box::from_raw(addr as u64 as *mut Font));
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
    (*(addr as u64 as *const Font))
        .face
        .glyph_index(codepoint)
        .map(|x| x.0 as i32)
        .unwrap_or(-1)
}

/// JNI wrapper for [`rustybuzz::Face::units_per_em`].
///
/// # Safety
///
/// `addr` must have previously been returned by [`Java_xyz_flirora_caxton_font_CaxtonInternal_createFont`]
/// and must not have been previously passed into [`Java_xyz_flirora_caxton_font_CaxtonInternal_destroyFont`].
// public static native int fontUnitsPerEm(long addr);
#[no_mangle]
pub unsafe extern "system" fn Java_xyz_flirora_caxton_font_CaxtonInternal_fontUnitsPerEm(
    _env: JNIEnv,
    _class: JClass,
    addr: jlong,
) -> jint {
    if addr == 0 {
        eprintln!("warn: was passed an address of 0; returning");
        return -1;
    }
    (*(addr as u64 as *const Font)).face.units_per_em()
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
    let shaping_result_class = env
        .find_class("xyz/flirora/caxton/font/ShapingResult")
        .unwrap();
    let shaping_result_ctor = env
        .get_method_id("xyz/flirora/caxton/font/ShapingResult", "<init>", "[II")
        .unwrap();

    let string = env
        .get_char_array_elements(s, ReleaseMode::NoCopyBack)
        .unwrap();
    let bidi_runs = env
        .get_int_array_elements(bidi_runs, ReleaseMode::NoCopyBack)
        .unwrap();
    let string = slice::from_raw_parts(string.as_ptr(), string.size().unwrap() as usize);
    let bidi_runs = slice::from_raw_parts(bidi_runs.as_ptr(), bidi_runs.size().unwrap() as usize);
    let font = &*(font_addr as u64 as *const Font);

    let num_bidi_runs = bidi_runs.len() / 2;
    let output = env
        .new_object_array(num_bidi_runs as i32, shaping_result_class, JObject::null())
        .unwrap();

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

        let data = env.new_int_array(6 * sr.data.len() as i32).unwrap();
        env.set_int_array_region(data, 0, sr.data_as_i32s())
            .unwrap();

        let object = env
            .new_object_unchecked(
                shaping_result_class,
                shaping_result_ctor,
                &[
                    JValue::Object(JObject::from_raw(data)),
                    JValue::Int(sr.total_width),
                ],
            )
            .unwrap();

        env.set_object_array_element(output, i as i32, object)
            .unwrap();

        buffer = shaped.clear();
    }

    output
}
