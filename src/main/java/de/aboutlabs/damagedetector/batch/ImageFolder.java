package de.aboutlabs.damagedetector.batch;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/** Driven adapter: finds the PNG/JPEG images below an input folder, recursively and in a stable order. */
@Component
class ImageFolder {

    List<Path> imagesBelow(Path folder) {
        if (!Files.isDirectory(folder)) {
            throw new IllegalArgumentException("Input folder does not exist: " + folder);
        }
        try (Stream<Path> files = Files.walk(folder)) {
            return files.filter(Files::isRegularFile).filter(ImageFolder::isImage).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static String mediaTypeOf(Path image) {
        return extensionOf(image).equals("png") ? "image/png" : "image/jpeg";
    }

    private static boolean isImage(Path file) {
        return List.of("png", "jpg", "jpeg").contains(extensionOf(file));
    }

    private static String extensionOf(Path file) {
        String name = file.getFileName().toString();
        return name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
