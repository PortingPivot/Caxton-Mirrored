package xyz.flirora.caxton.font;

import xyz.flirora.bindings.*;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Holds information related to a single font file.
 */
public class CaxtonFont implements AutoCloseable {
    private static final byte[] SALT = {-0x1A, 0x26, 0x69, 0x11};

    private final SWIGTYPE_p_hb_font_t font;

    public CaxtonFont(InputStream input) throws IOException {
        SWIGTYPE_p_hb_blob_t blob = null;
        SWIGTYPE_p_hb_face_t face = null;

        try {
            byte[] readInput = input.readAllBytes();

            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(readInput);
            byte[] hash = digest.digest(SALT);
            String cacheFolder = Base64.getEncoder().encodeToString(hash);

            blob = Harfbuzz.hb_blob_create(readInput, hb_memory_mode_t.HB_MEMORY_MODE_DUPLICATE, null, null);
            face = Harfbuzz.hb_face_create(blob, 0);
            this.font = Harfbuzz.hb_font_create(face);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            Harfbuzz.hb_blob_destroy(blob);
            Harfbuzz.hb_face_destroy(face);
        }
    }

    @Override
    public void close() throws Exception {
        Harfbuzz.hb_font_destroy(this.font);
    }
}
