package com.harbr.common.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class LocalStorageService implements FileStorageService {

    private final Path storageRoot;

    public LocalStorageService(@Value("${harbr.storage.local-path:./uploads}") String storagePath) {
        this.storageRoot = Paths.get(storagePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageRoot);
        } catch (IOException e) {
            throw new RuntimeException("Could not create storage directory", e);
        }
    }

    @Override
    public String store(String directory, String filename, InputStream content, String contentType) {
        try {
            Path dir = storageRoot.resolve(directory);
            Files.createDirectories(dir);

            String extension = "";
            int dotIndex = filename.lastIndexOf('.');
            if (dotIndex > 0) {
                extension = filename.substring(dotIndex);
            }
            String storedName = UUID.randomUUID() + extension;

            Path targetFile = dir.resolve(storedName);
            Files.copy(content, targetFile, StandardCopyOption.REPLACE_EXISTING);

            log.info("Stored file: {}/{}", directory, storedName);
            return "/uploads/" + directory + "/" + storedName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    @Override
    public void delete(String url) {
        try {
            String relativePath = url.replace("/uploads/", "");
            Path file = storageRoot.resolve(relativePath);
            Files.deleteIfExists(file);
            log.info("Deleted file: {}", relativePath);
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", url, e);
        }
    }
}