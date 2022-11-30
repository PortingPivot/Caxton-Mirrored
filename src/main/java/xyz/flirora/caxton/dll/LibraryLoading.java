package xyz.flirora.caxton.dll;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.lwjgl.system.Platform;
import org.slf4j.Logger;
import xyz.flirora.caxton.CaxtonModClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class LibraryLoading {
    private static final Text UNSUPPORTED_PLATFORM = Text.translatable("caxton.gui.unsupportedPlatform").formatted(Formatting.BOLD);
    private static final Text LIBRARY_LOADING_FAILED = Text.translatable("caxton.gui.libraryLoadingFailed").formatted(Formatting.BOLD);
    private static final Text UNSUPPORTED_PLATFORM_DESC = Text.translatable("caxton.gui.unsupportedPlatform.description");
    private static final Text UNSUPPORTED_PLATFORM_DESC_OSX =
            Text.translatable("caxton.gui.unsupportedPlatform.description.osx")
                    .append(Text.translatable("caxton.gui.unsupportedPlatform.description.osx2")
                            .formatted(Formatting.BOLD));
    private static final Text LIBRARY_LOADING_FAILED_DESC = Text.translatable("caxton.gui.libraryLoadingFailed.description");
    public static String ISSUES_URL = "https://gitlab.com/Kyarei/caxton/-/issues";
    private static final Text LIBRARY_LOADING_FAILED_LINK = Text.translatable("caxton.gui.libraryLoadingFailed.link", ISSUES_URL);
    public static String OS_SUPPORT_URL = "https://gitlab.com/Kyarei/caxton#os-support";
    private static final Text UNSUPPORTED_PLATFORM_LINK = Text.translatable("caxton.gui.unsupportedPlatform.link", OS_SUPPORT_URL);

    private static Exception loadingException = new IllegalStateException("library not yet loaded");

    public static Exception getLoadingException() {
        return loadingException;
    }

    public static boolean isLibraryLoaded() {
        return loadingException == null;
    }

    public static void loadNativeLibrary(Logger logger) {
        logger.info(
                "Extracting and loading library for {} / {}",
                System.getProperty("os.name"),
                System.getProperty("os.arch"));

        File runDirectory = MinecraftClient.getInstance().runDirectory;

        String soName = System.mapLibraryName("caxton_impl");

        try (InputStream libStream = CaxtonModClient.class.getResourceAsStream("/" + soName)) {
            if (libStream == null) {
                throw new UnsupportedPlatformException("Could not find " + soName);
            }

            File tmp = new File(runDirectory, soName);
            try (var output = new FileOutputStream(tmp)) {
                libStream.transferTo(output);
            }

            System.load(tmp.toString());
            loadingException = null;
        } catch (Exception e) {
            logger.error("Failed to load library", e);
            loadingException = e;
        }
    }

    private static boolean isMacOs() {
        return Platform.get() == Platform.MACOSX;
    }

    private static Text getTitle(Exception e) {
        return e instanceof UnsupportedPlatformException ? UNSUPPORTED_PLATFORM : LIBRARY_LOADING_FAILED;
    }

    private static Text getDescriptionText(Exception e) {
        boolean unsupported = e instanceof UnsupportedPlatformException;
        Text description = unsupported ? (isMacOs() ? UNSUPPORTED_PLATFORM_DESC_OSX : UNSUPPORTED_PLATFORM_DESC) : LIBRARY_LOADING_FAILED_DESC;
        Text link = unsupported ? UNSUPPORTED_PLATFORM_LINK : LIBRARY_LOADING_FAILED_LINK;
        return Text.translatable("caxton.gui.failDescription",
                description,
                System.getProperty("os.name"),
                System.getProperty("os.arch"),
                link);
    }

    public static Screen nativeLibraryLoadFailedScreen(BooleanConsumer callback, Exception e) {
        return new ConfirmLinkScreen(callback, getTitle(e), getDescriptionText(e), OS_SUPPORT_URL, ScreenTexts.ACKNOWLEDGE, true);
    }

    public static void showNativeLibraryLoadFailedScreen(MinecraftClient client, Exception e) {
        Screen screen = client.currentScreen;
        client.setScreen(nativeLibraryLoadFailedScreen(confirmed -> {
            if (confirmed) {
                String url = e instanceof UnsupportedPlatformException ? OS_SUPPORT_URL : ISSUES_URL;
                Util.getOperatingSystem().open(url);
            }
            client.setScreen(screen);
        }, e));
    }
}
