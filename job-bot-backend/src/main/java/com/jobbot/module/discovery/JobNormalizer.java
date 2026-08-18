package com.jobbot.module.discovery;

import com.jobbot.module.discovery.adapter.DiscoveredPosting;
import com.jobbot.module.matching.LocationEngine;
import com.jobbot.module.matching.WorkMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Normalizes a raw {@link DiscoveredPosting} into a {@link JobPosting} (spec §12).
 * Reuses the Phase-2 {@link LocationEngine} for city + work-mode normalization.
 * Ambiguous values become UNKNOWN — never guessed (§12).
 */
@Component
@RequiredArgsConstructor
public class JobNormalizer {

    private final LocationEngine locationEngine;

    // e.g. "2-4 years", "2 to 5 yrs", "3+ years"
    private static final Pattern EXP_RANGE = Pattern.compile(
            "(\\d+)\\s*(?:-|to|–)\\s*(\\d+)\\s*\\+?\\s*(?:years|yrs|year)", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXP_MIN = Pattern.compile(
            "(\\d+)\\s*\\+\\s*(?:years|yrs|year)", Pattern.CASE_INSENSITIVE);

    public JobPosting normalize(DiscoveredPosting raw) {
        String city = locationEngine.normalizeCity(raw.location());
        WorkMode mode = resolveWorkMode(raw);
        int[] exp = extractExperience(raw.description());

        JobPosting posting = JobPosting.builder()
                .source(raw.source())
                .externalId(raw.externalId())
                .title(cleanTitle(raw.title()))
                .company(raw.company())
                .location(city)
                .remoteType(mode.name())
                .employmentType(normalizeEmploymentType(raw.employmentTypeRaw()))
                .description(raw.description())
                .sourceUrl(raw.jobUrl())
                .applicationUrl(raw.applicationUrl())
                .postingDate(raw.postingDate())
                .minimumExperience(exp != null ? exp[0] : null)
                .maximumExperience(exp != null ? exp[1] : null)
                .build();
        posting.setNormalizedHash(computeHash(raw.company(), posting.getTitle(), city));
        return posting;
    }

    /** Resolve work mode from an explicit remote hint or the location text (§12). */
    WorkMode resolveWorkMode(DiscoveredPosting raw) {
        WorkMode fromHint = locationEngine.parseWorkMode(raw.remoteHint());
        if (fromHint != WorkMode.UNKNOWN) return fromHint;
        return locationEngine.parseWorkMode(raw.location());
    }

    String normalizeEmploymentType(String raw) {
        if (raw == null) return "UNKNOWN";
        String t = raw.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "").replace(" ", "");
        if (t.contains("fulltime") || t.equals("permanent")) return "FULL_TIME";
        if (t.contains("parttime")) return "PART_TIME";
        if (t.contains("contract") || t.contains("temporary")) return "CONTRACT";
        if (t.contains("intern")) return "INTERNSHIP";
        return "UNKNOWN";
    }

    /** Returns {min,max} years, or null if not confidently determinable (§12). */
    int[] extractExperience(String description) {
        if (description == null) return null;
        Matcher r = EXP_RANGE.matcher(description);
        if (r.find()) {
            int a = Integer.parseInt(r.group(1));
            int b = Integer.parseInt(r.group(2));
            return new int[]{Math.min(a, b), Math.max(a, b)};
        }
        Matcher m = EXP_MIN.matcher(description);
        if (m.find()) {
            int a = Integer.parseInt(m.group(1));
            return new int[]{a, a + 3};
        }
        return null;
    }

    private String cleanTitle(String title) {
        return title == null ? null : title.trim().replaceAll("\\s+", " ");
    }

    /** Deterministic dedup key: sha-256 of company|title|city (all normalized) (§13). */
    public String computeHash(String company, String title, String city) {
        String key = (safe(company) + "|" + safe(title) + "|" + safe(city))
                .toLowerCase(Locale.ROOT);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(key.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(key.hashCode());
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}

