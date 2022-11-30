package xyz.flirora.caxton.dll;

public class RustPlatform {
    public static String forCurrent() {
        String target = System.getProperty("xyz.flirora.caxton.rustTarget");
        if (target != null) {
            return target;
        }

        String osName = System.getProperty("os.name");
        String osArch = System.getProperty("os.arch");
        String osName3, osArch3;

        if (osName.startsWith("Windows")) {
            osName3 = "pc-windows-gnu";
        } else if (osName.startsWith("Linux")) {
            osName3 = "unknown-linux-gnu";
        } else if (osName.startsWith("FreeBSD")) {
            osName3 = "unknown-freebsd";
        } else if (osName.startsWith("Mac OS X") || osName.startsWith("Darwin")) {
            osName3 = "apple-darwin";
        } else {
            throw new UnsupportedPlatformException("Cannot determine Rust platform name: unrecognized OS name " + osName);
        }

        osArch3 = switch (osArch) {
            case "x86_64", "amd64" -> "x86_64";
            case "x86", "i686" -> "i686";
            case "aarch64" -> "aarch64";
            default ->
                    throw new UnsupportedPlatformException("Cannot determine Rust platform name: unrecognized architecture name" + osArch);
        };

        return osArch3 + "-" + osName3;
    }
}
