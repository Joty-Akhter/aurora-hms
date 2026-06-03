package com.easyops.organization.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Persists organization logos on disk and serves them via GET /api/organizations/{id}/logo.
 */
@Service
@Slf4j
public class OrganizationLogoService {

    private static final long MAX_BYTES = 2 * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif", "image/svg+xml"
    );

    @Value("${easyops.organization.logo-storage-dir:uploads/organization-logos}")
    private String storageDir;

    public void store(UUID orgId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("Logo must be at most 2 MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Only JPEG, PNG, WebP, GIF, or SVG images are allowed");
        }

        Path dir = Paths.get(storageDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);

        String ext = extensionForContentType(contentType);
        deleteExisting(orgId, dir);

        Path target = dir.resolve(orgId + ext);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        log.info("Stored organization logo at {}", target);
    }

    private static String extensionForContentType(String contentType) {
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            case "image/svg+xml" -> ".svg";
            default -> ".bin";
        };
    }

    private void deleteExisting(UUID orgId, Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return;
        }
        String prefix = orgId.toString() + ".";
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(p -> p.getFileName().toString().startsWith(prefix)).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    log.warn("Could not delete old logo {}", p, e);
                }
            });
        }
    }

    public Optional<Resource> loadAsResource(UUID orgId) throws IOException {
        Path dir = Paths.get(storageDir).toAbsolutePath().normalize();
        if (!Files.isDirectory(dir)) {
            return Optional.empty();
        }
        Optional<Path> file = findLogoFile(orgId, dir);
        if (file.isEmpty()) {
            return Optional.empty();
        }
        Path path = file.get();
        Resource resource = new UrlResource(path.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            return Optional.empty();
        }
        return Optional.of(resource);
    }

    private Optional<Path> findLogoFile(UUID orgId, Path dir) throws IOException {
        String prefix = orgId.toString() + ".";
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().startsWith(prefix)).findFirst();
        }
    }

    public MediaType mediaTypeForOrg(UUID orgId) throws IOException {
        Path dir = Paths.get(storageDir).toAbsolutePath().normalize();
        Optional<Path> file = findLogoFile(orgId, dir);
        if (file.isEmpty()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        String name = file.get().getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        }
        if (name.endsWith(".gif")) {
            return MediaType.IMAGE_GIF;
        }
        if (name.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        if (name.endsWith(".svg")) {
            return MediaType.parseMediaType("image/svg+xml");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
