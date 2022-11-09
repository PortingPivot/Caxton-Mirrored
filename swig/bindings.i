#include <stdint.h>

#define HB_NO_SINGLE_HEADER_ERROR

%module harfbuzz
%{
#include "/usr/include/harfbuzz/hb.h"
%}

%apply (char *STRING, size_t LENGTH) { (const char *data, unsigned int length) };

%newobject hb_blob_create;
%delobject hb_blob_destroy;
%newobject hb_face_create;
%delobject hb_face_destroy;
%newobject hb_font_create;
%delobject hb_font_destroy;

%include "/usr/include/harfbuzz/hb-common.h"
%include "/usr/include/harfbuzz/hb-blob.h"
%include "/usr/include/harfbuzz/hb-buffer.h"
%include "/usr/include/harfbuzz/hb-face.h"
%include "/usr/include/harfbuzz/hb-font.h"
%include "/usr/include/harfbuzz/hb-shape.h"