package com.joyeria.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService service;

    @BeforeEach
    void setUp() {
        service = new FileStorageService();
        ReflectionTestUtils.setField(service, "uploadDir", tempDir.toString());
    }

    @Test
    void store_ValidImage_SavesFileAndReturnsUrl() {
        MockMultipartFile file = new MockMultipartFile("file", "anillo.png", "image/png", new byte[]{1, 2, 3});

        String url = service.store(file);

        assertTrue(url.startsWith("/uploads/"));
        assertTrue(url.endsWith(".png"));
        Path saved = tempDir.resolve(url.substring("/uploads/".length()));
        assertTrue(Files.exists(saved));
    }

    @Test
    void store_WithoutExtension_UsesTypeExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "foto", "image/webp", new byte[]{9});

        String url = service.store(file);

        assertTrue(url.endsWith(".webp"));
    }

    @Test
    void store_Empty_Throws() {
        MockMultipartFile file = new MockMultipartFile("file", "x.png", "image/png", new byte[0]);

        assertThrows(IllegalArgumentException.class, () -> service.store(file));
    }

    @Test
    void store_DisallowedType_Throws() {
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[]{1});

        assertThrows(IllegalArgumentException.class, () -> service.store(file));
    }
}
