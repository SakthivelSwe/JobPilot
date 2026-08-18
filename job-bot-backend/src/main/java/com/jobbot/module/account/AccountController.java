package com.jobbot.module.account;

import com.jobbot.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Data export &amp; ownership API (spec §69/§70). The user can export everything and reset
 * the app. Nothing here is destructive without an explicit call.
 */
@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final ExportService exportService;

    @GetMapping("/export/applications.csv")
    public ResponseEntity<String> applicationsCsv() {
        return csv(exportService.applicationsCsv(), "applications.csv");
    }

    @GetMapping("/export/jobs.csv")
    public ResponseEntity<String> jobsCsv() {
        return csv(exportService.jobsCsv(), "jobs.csv");
    }

    @GetMapping("/export/data.json")
    public ResponseEntity<Map<String, Object>> fullExport() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"jobpilot-export.json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(exportService.fullExport());
    }

    /** Delete all personal data (keeps source/company configuration). */
    @PostMapping("/reset")
    public ApiResponse<Map<String, Long>> reset() {
        return ApiResponse.ok(exportService.resetPersonalData(), "Personal data reset");
    }

    /** Delete only stored resume file metadata. */
    @DeleteMapping("/resume-files")
    public ApiResponse<Map<String, Long>> deleteResumeFiles() {
        long n = exportService.deleteResumeFiles();
        return ApiResponse.ok(Map.of("deleted", n), "Resume files deleted");
    }

    private ResponseEntity<String> csv(String body, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(body);
    }
}

