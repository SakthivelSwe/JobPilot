package com.jobbot.module.storage;

import com.jobbot.common.exception.JobBotException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Local-filesystem storage for dev. NOT for Render prod (ephemeral FS). A Supabase
 * Storage implementation replaces this in a later phase via the same interface.
 */
@Service
@Slf4j
public class LocalStorageService implements StorageService {

    private final Path root;

    public LocalStorageService(@Value("${app.storage.local.dir:./data/storage}") String dir) {
        this.root = Paths.get(dir).toAbsolutePath();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new JobBotException("Cannot create storage dir: " + root);
        }
    }

    @Override
    public String store(String folder, String fileName, byte[] bytes) {
        try {
            String safeFolder = folder == null ? "misc" : folder.replaceAll("[^a-zA-Z0-9_-]", "_");
            Path dir = root.resolve(safeFolder);
            Files.createDirectories(dir);
            String key = safeFolder + "/" + UUID.randomUUID() + "-" + sanitize(fileName);
            Path target = root.resolve(key);
            Files.write(target, bytes);
            log.info("Stored {} bytes at {}", bytes.length, key);
            return key;
        } catch (IOException e) {
            throw new JobBotException("Failed to store file: " + e.getMessage());
        }
    }

    @Override
    public String url(String storagePath) {
        return root.resolve(storagePath).toUri().toString();
    }

    private static String sanitize(String fileName) {
        if (fileName == null) return "file";
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}

