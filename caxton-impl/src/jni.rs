use std::{mem, path::PathBuf, slice};

use jni::{
    objects::{JByteBuffer, JClass, JString},
    sys::jlong,
    JNIEnv,
};

use crate::font::Font;

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
