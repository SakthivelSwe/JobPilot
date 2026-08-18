package com.jobbot.module.activity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Records + reads real system events (rule 55/56/72). Recording is best-effort and
 * never breaks the calling action — a failed event write is logged and swallowed.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityService {

    private final ActivityRepository repository;

    public void record(String type, String title, String detail, String entityType, UUID entityId) {
        try {
            repository.save(ActivityEvent.builder()
                    .type(type).title(title).detail(detail)
                    .entityType(entityType).entityId(entityId)
                    .build());
        } catch (Exception e) {
            log.warn("Activity record failed ({}: {}): {}", type, title, e.getMessage());
        }
    }

    public void record(String type, String title, String detail) {
        record(type, title, detail, null, null);
    }

    public List<ActivityEvent> recent(int limit) {
        int safe = Math.max(1, Math.min(limit, 100));
        return repository.findByOrderByCreatedAtDesc(PageRequest.of(0, safe));
    }
}

