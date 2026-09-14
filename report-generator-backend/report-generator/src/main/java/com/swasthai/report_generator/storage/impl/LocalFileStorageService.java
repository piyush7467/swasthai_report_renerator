package com.swasthai.report_generator.storage.impl;

import com.swasthai.report_generator.storage.FileStorageService;
import com.swasthai.report_generator.storage.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocalFileStorageService
        implements FileStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of(
                    "image/png",
                    "image/jpeg",
                    "image/webp"
            );

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of(
                    "png",
                    "jpg",
                    "jpeg",
                    "webp"
            );

    private final StorageProperties properties;

    @Override
    public String storeOrganizationLogo(
            String organizationRefId,
            MultipartFile file
    ) {

        return store(
                "organization/" + organizationRefId + "/logo",
                file
        );
    }

    @Override
    public String storeOrganizationSignature(
            String organizationRefId,
            MultipartFile file
    ) {

        return store(
                "organization/" + organizationRefId + "/signature",
                file
        );
    }

    private String store(
            String relativeDirectory,
            MultipartFile file
    ) {

        validateFile(file);

        String extension =
                resolveExtension(file.getOriginalFilename());

        String filename =
                UUID.randomUUID() + "." + extension;

        Path root =
                Paths.get(properties.getRootPath())
                        .toAbsolutePath()
                        .normalize();

        Path directory =
                root.resolve(relativeDirectory)
                        .normalize();

        if (!directory.startsWith(root)) {
            throw new IllegalStateException(
                    "Invalid storage directory."
            );
        }

        try {

            Files.createDirectories(directory);

            Path target =
                    directory.resolve(filename)
                            .normalize();

            if (!target.startsWith(directory)) {
                throw new IllegalStateException(
                        "Invalid storage path."
                );
            }

            try (InputStream inputStream = file.getInputStream()) {

                Files.copy(
                        inputStream,
                        target,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            /*
             * Verify that the stored file is actually an image.
             */
            verifyStoredImage(target);

            return root.relativize(target)
                    .toString()
                    .replace('\\', '/');

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Unable to store image.",
                    exception
            );
        }
    }

    @Override
    public byte[] load(String storageKey) {

        Path path = resolveSafe(storageKey);

        try {

            if (!Files.exists(path)
                    || !Files.isRegularFile(path)) {

                throw new IllegalArgumentException(
                        "Stored file does not exist."
                );
            }

            return Files.readAllBytes(path);

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Unable to load stored file.",
                    exception
            );
        }
    }

    @Override
    public void delete(String storageKey) {

        if (storageKey == null || storageKey.isBlank()) {
            return;
        }

        Path path = resolveSafe(storageKey);

        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to delete stored file.",
                    exception
            );
        }
    }

    @Override
    public boolean exists(String storageKey) {

        if (storageKey == null || storageKey.isBlank()) {
            return false;
        }

        Path path = resolveSafe(storageKey);

        return Files.isRegularFile(path);
    }

    private Path resolveSafe(String storageKey) {

        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Storage key is required."
            );
        }

        if (storageKey.contains("..")
                || storageKey.contains("\\")
                || storageKey.startsWith("/")
                || storageKey.startsWith("\\")) {

            throw new IllegalArgumentException(
                    "Invalid storage key."
            );
        }

        Path root =
                Paths.get(properties.getRootPath())
                        .toAbsolutePath()
                        .normalize();

        Path resolved =
                root.resolve(storageKey)
                        .normalize();

        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException(
                    "Invalid storage key."
            );
        }

        return resolved;
    }

    private void validateFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Image file is required."
            );
        }

        if (file.getSize() > properties.getMaxImageSizeBytes()) {
            throw new IllegalArgumentException(
                    "Image file is too large."
            );
        }

        String contentType =
                file.getContentType();

        if (contentType == null
                || !ALLOWED_CONTENT_TYPES.contains(
                contentType.toLowerCase(Locale.ROOT))) {

            throw new IllegalArgumentException(
                    "Only PNG, JPEG, and WebP images are allowed."
            );
        }

        String extension =
                resolveExtension(file.getOriginalFilename());

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(
                    "Invalid image file extension."
            );
        }

        try (InputStream inputStream = file.getInputStream()) {

            BufferedImage image =
                    ImageIO.read(inputStream);

            if (image == null) {
                throw new IllegalArgumentException(
                        "Uploaded file is not a valid image."
                );
            }

            if (image.getWidth() <= 0
                    || image.getHeight() <= 0) {

                throw new IllegalArgumentException(
                        "Invalid image dimensions."
                );
            }

        } catch (IOException exception) {

            throw new IllegalArgumentException(
                    "Unable to validate image.",
                    exception
            );
        }
    }

    private void verifyStoredImage(Path path) {

        try {

            BufferedImage image =
                    ImageIO.read(path.toFile());

            if (image == null) {

                Files.deleteIfExists(path);

                throw new IllegalArgumentException(
                        "Stored file is not a valid image."
                );
            }

        } catch (IOException exception) {

            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
            }

            throw new IllegalStateException(
                    "Unable to verify stored image.",
                    exception
            );
        }
    }

    private String resolveExtension(
            String originalFilename
    ) {

        if (originalFilename == null
                || originalFilename.isBlank()) {

            throw new IllegalArgumentException(
                    "Image filename is required."
            );
        }

        String filename =
                Paths.get(originalFilename)
                        .getFileName()
                        .toString();

        int dot =
                filename.lastIndexOf('.');

        if (dot <= 0
                || dot == filename.length() - 1) {

            throw new IllegalArgumentException(
                    "Image file extension is required."
            );
        }

        return filename
                .substring(dot + 1)
                .toLowerCase(Locale.ROOT);
    }
}