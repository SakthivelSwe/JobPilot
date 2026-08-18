package com.jobbot.module.candidate.parse;

import com.jobbot.common.exception.JobBotException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Extracts plain text from an uploaded resume. Supports PDF (PDFBox),
 * DOCX (POI XWPF), DOC (POI HWPF) and TXT. Deterministic, offline, free.
 */
@Component
@Slf4j
public class ResumeParser {

    public String extractText(String fileName, String mimeType, byte[] bytes) {
        String kind = detectKind(fileName, mimeType);
        try {
            return switch (kind) {
                case "pdf" -> extractPdf(bytes);
                case "docx" -> extractDocx(bytes);
                case "doc" -> extractDoc(bytes);
                case "txt" -> new String(bytes, StandardCharsets.UTF_8);
                default -> throw new JobBotException("Unsupported resume format: " + fileName);
            };
        } catch (JobBotException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Resume text extraction failed for {}: {}", fileName, e.getMessage());
            throw new JobBotException("Could not read resume file: " + e.getMessage());
        }
    }

    private String detectKind(String fileName, String mimeType) {
        String fn = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        String mt = mimeType == null ? "" : mimeType.toLowerCase(Locale.ROOT);
        if (fn.endsWith(".pdf") || mt.contains("pdf")) return "pdf";
        if (fn.endsWith(".docx") || mt.contains("openxmlformats-officedocument.wordprocessingml")) return "docx";
        if (fn.endsWith(".doc") || mt.equals("application/msword")) return "doc";
        if (fn.endsWith(".txt") || mt.startsWith("text/")) return "txt";
        return "unknown";
    }

    private String extractPdf(byte[] bytes) throws Exception {
        try (PDDocument doc = PDDocument.load(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    private String extractDocx(byte[] bytes) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(bytes));
             XWPFWordExtractor ex = new XWPFWordExtractor(doc)) {
            return ex.getText();
        }
    }

    private String extractDoc(byte[] bytes) throws Exception {
        try (WordExtractor ex = new WordExtractor(new ByteArrayInputStream(bytes))) {
            return ex.getText();
        }
    }
}

