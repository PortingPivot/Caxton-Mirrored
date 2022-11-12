/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class xyz_flirora_caxton_font_CaxtonInternal */

#ifndef _Included_xyz_flirora_caxton_font_CaxtonInternal
#define _Included_xyz_flirora_caxton_font_CaxtonInternal
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     xyz_flirora_caxton_font_CaxtonInternal
 * Method:    createFont
 * Signature: (Ljava/nio/ByteBuffer;Ljava/lang/String;Lxyz/flirora/caxton/font/CaxtonFontOptions;)J
 */
JNIEXPORT jlong JNICALL Java_xyz_flirora_caxton_font_CaxtonInternal_createFont
  (JNIEnv *, jclass, jobject, jstring, jobject);

/*
 * Class:     xyz_flirora_caxton_font_CaxtonInternal
 * Method:    destroyFont
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_xyz_flirora_caxton_font_CaxtonInternal_destroyFont
  (JNIEnv *, jclass, jlong);

/*
 * Class:     xyz_flirora_caxton_font_CaxtonInternal
 * Method:    fontGlyphIndex
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_xyz_flirora_caxton_font_CaxtonInternal_fontGlyphIndex
  (JNIEnv *, jclass, jlong, jint);

/*
 * Class:     xyz_flirora_caxton_font_CaxtonInternal
 * Method:    fontMetrics
 * Signature: (J)[S
 */
JNIEXPORT jshortArray JNICALL Java_xyz_flirora_caxton_font_CaxtonInternal_fontMetrics
  (JNIEnv *, jclass, jlong);

/*
 * Class:     xyz_flirora_caxton_font_CaxtonInternal
 * Method:    fontAtlasSize
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_xyz_flirora_caxton_font_CaxtonInternal_fontAtlasSize
  (JNIEnv *, jclass, jlong);

/*
 * Class:     xyz_flirora_caxton_font_CaxtonInternal
 * Method:    fontAtlasLocations
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_xyz_flirora_caxton_font_CaxtonInternal_fontAtlasLocations
  (JNIEnv *, jclass, jlong);

/*
 * Class:     xyz_flirora_caxton_font_CaxtonInternal
 * Method:    fontBboxes
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_xyz_flirora_caxton_font_CaxtonInternal_fontBboxes
  (JNIEnv *, jclass, jlong);

/*
 * Class:     xyz_flirora_caxton_font_CaxtonInternal
 * Method:    fontAtlasNumPages
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_xyz_flirora_caxton_font_CaxtonInternal_fontAtlasNumPages
  (JNIEnv *, jclass, jlong);

/*
 * Class:     xyz_flirora_caxton_font_CaxtonInternal
 * Method:    fontAtlasPage
 * Signature: (JI)J
 */
JNIEXPORT jlong JNICALL Java_xyz_flirora_caxton_font_CaxtonInternal_fontAtlasPage
  (JNIEnv *, jclass, jlong, jint);

/*
 * Class:     xyz_flirora_caxton_font_CaxtonInternal
 * Method:    shape
 * Signature: (J[C[I)[Lxyz/flirora/caxton/font/ShapingResult;
 */
JNIEXPORT jobjectArray JNICALL Java_xyz_flirora_caxton_font_CaxtonInternal_shape
  (JNIEnv *, jclass, jlong, jcharArray, jintArray);

#ifdef __cplusplus
}
#endif
#endif
