package com.joyeria.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

/**
 * Almacenamiento local de imágenes subidas por el admin. Devuelve una URL
 * relativa servida por {@link com.joyeria.config.WebConfig} en {@code /uploads/**}.
 * Si se configuran credenciales de Cloudinary, se puede sustituir esta implementación.
 */
@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Formato no permitido (usa JPEG, PNG, WEBP o GIF)");
        }
        String filename = UUID.randomUUID().toString().replace("-", "") + extensionFor(file.getOriginalFilename(), contentType);
        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(filename));
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar la imagen", e);
        }
        return "/uploads/" + filename;
    }

    private String extensionFor(String originalName, String contentType) {
        if (originalName != null) {
            int dot = originalName.lastIndexOf('.');
            if (dot >= 0 && dot < originalName.length() - 1) {
                String ext = originalName.substring(dot).toLowerCase();
                if (ext.matches("\\.[a-z0-9]{1,5}")) {
                    return ext;
                }
            }
        }
        return switch (contentType.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }
}
