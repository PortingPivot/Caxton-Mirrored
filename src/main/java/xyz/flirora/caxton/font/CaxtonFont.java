package xyz.flirora.caxton.font;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Holds information related to a single font file.
 * <p>
 * At the moment, this uses functionality from AWT. This unfortunately means
 * that this mod might not work properly in macOS, in which case go R.L. yourself.
 * <p>
 * Still, no references to AWT classes should be made outside this class, in case
 * we discover a way to shape text in Java without AWT.
 */
public class CaxtonFont {
    private static final byte[] SALT = {-0x1A, 0x26, 0x69, 0x11};

    private final java.awt.Font font;

    public CaxtonFont(InputStream input) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(input.readAllBytes());
            byte[] hash = digest.digest(SALT);
            String cacheFolder = Base64.getEncoder().encodeToString(hash);
            input.reset();
            this.font = java.awt.Font.createFonts(input)[0];
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
