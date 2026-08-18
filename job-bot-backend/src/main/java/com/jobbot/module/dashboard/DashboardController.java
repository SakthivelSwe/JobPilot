package com.jobbot.module.dashboard;

import com.jobbot.common.ApiResponse;
import com.jobbot.module.application.Application;
import com.jobbot.module.application.ApplicationRepository;
import com.jobbot.module.resume.Resume;
import com.jobbot.module.resume.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private static final Set<String> RESPONSE_STATUSES =
            Set.of("viewed", "shortlisted", "interview", "offer");

    private final ApplicationRepository applicationRepository;
    private final ResumeService resumeService;

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        List<Application> all = applicationRepository.findAll();
        long total = all.size();
        long interviews = all.stream().filter(a -> "interview".equals(a.getStatus())).count();
        long offers = all.stream().filter(a -> "offer".equals(a.getStatus())).count();
        long rejections = all.stream().filter(a -> "rejected".equals(a.getStatus())).count();

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        long todayApplied = all.stream()
                .filter(a -> a.getAppliedAt() != null
                        && a.getAppliedAt().atZoneSameInstant(ZoneId.of("Asia/Kolkata"))
                        .toLocalDate().equals(today))
                .count();

        Map<String, Long> byPlatform = all.stream()
                .filter(a -> a.getPlatform() != null)
                .collect(Collectors.groupingBy(Application::getPlatform, Collectors.counting()));
        Map<String, Long> byStatus = all.stream()
                .collect(Collectors.groupingBy(Application::getStatus, Collectors.counting()));

        double successRate = total == 0 ? 0 : Math.round((interviews + offers) * 1000.0 / total) / 10.0;

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalApplied", total);
        stats.put("interviews", interviews);
        stats.put("offers", offers);
        stats.put("activeRejections", rejections);
        stats.put("todayApplied", todayApplied);
        stats.put("successRate", successRate);
        stats.put("byPlatform", byPlatform);
        stats.put("byStatus", byStatus);
        return ApiResponse.ok(stats);
    }

    /** Learning engine: which resume converts best? */
    @GetMapping("/resume-performance")
    public ApiResponse<List<Map<String, Object>>> resumePerformance() {
        List<Application> all = applicationRepository.findAll();
        Map<UUID, String> names = new HashMap<>();
        for (Resume r : resumeService.findAll()) {
            names.put(r.getId(), r.getName());
        }

        Map<UUID, List<Application>> byResume = all.stream()
                .filter(a -> a.getResumeId() != null)
                .collect(Collectors.groupingBy(Application::getResumeId));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<UUID, List<Application>> e : byResume.entrySet()) {
            List<Application> apps = e.getValue();
            long count = apps.size();
            long responses = apps.stream().filter(a -> RESPONSE_STATUSES.contains(a.getStatus())).count();
            long interviews = apps.stream().filter(a -> "interview".equals(a.getStatus())).count();
            long offers = apps.stream().filter(a -> "offer".equals(a.getStatus())).count();

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("resumeId", e.getKey());
            row.put("resumeName", names.getOrDefault(e.getKey(), "Unknown"));
            row.put("applications", count);
            row.put("responses", responses);
            row.put("interviews", interviews);
            row.put("offers", offers);
            row.put("responseRate", count == 0 ? 0 : Math.round(responses * 1000.0 / count) / 10.0);
            row.put("interviewRate", count == 0 ? 0 : Math.round(interviews * 1000.0 / count) / 10.0);
            rows.add(row);
        }
        rows.sort((a, b) -> Double.compare((double) b.get("interviewRate"), (double) a.get("interviewRate")));
        return ApiResponse.ok(rows);
    }
}

