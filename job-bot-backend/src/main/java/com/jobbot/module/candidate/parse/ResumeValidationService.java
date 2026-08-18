package com.jobbot.module.candidate.parse;

import com.jobbot.common.exception.JobBotException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

/**
 * Validates uploaded resume files before parsing (spec §4 file validation).
 */
@Component
public class ResumeValidationService {

    private static final Set<String> ALLOWED_EXT = Set.of("pdf", "doc", "docx", "txt");

    @Value("${app.resume.max-size-bytes:5242880}") // 5 MB
    private long maxSizeBytes;

    public void validate(String fileName, long size, byte[] bytes) {
        if (fileName == null || fileName.isBlank()) {
            throw new JobBotException("File name is required");
        }
        if (bytes == null || bytes.length == 0) {
            throw new JobBotException("Uploaded file is empty");
        }
        if (size > maxSizeBytes) {
            throw new JobBotException("File too large. Max " + (maxSizeBytes / (1024 * 1024)) + " MB");
        }
        String ext = extension(fileName);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new JobBotException("Unsupported format '." + ext + "'. Allowed: PDF, DOC, DOCX, TXT");
        }
    }

    private String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }
}

