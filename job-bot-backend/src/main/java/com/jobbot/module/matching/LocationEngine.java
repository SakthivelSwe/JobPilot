package com.jobbot.module.matching;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Deterministic location & work-mode engine (spec §17, with normalization rules
 * from §12). No AI, no external calls.
 *
 * <p>Key principle (§12): when the input is ambiguous we return
 * {@link WorkMode#UNKNOWN} / {@link LocationMatch#UNKNOWN} rather than guessing.
 */
@Component
public class LocationEngine {

    /** Canonical city aliases (normalize alternate spellings → canonical). */
    private static final Map<String, String> CITY_ALIASES = Map.ofEntries(
            Map.entry("bangalore", "Bengaluru"),
            Map.entry("bengaluru", "Bengaluru"),
            Map.entry("blr", "Bengaluru"),
            Map.entry("gurgaon", "Gurugram"),
            Map.entry("gurugram", "Gurugram"),
            Map.entry("bombay", "Mumbai"),
            Map.entry("mumbai", "Mumbai"),
            Map.entry("madras", "Chennai"),
            Map.entry("chennai", "Chennai"),
            Map.entry("calcutta", "Kolkata"),
            Map.entry("kolkata", "Kolkata"),
            Map.entry("hyderabad", "Hyderabad"),
            Map.entry("hyd", "Hyderabad"),
            Map.entry("pune", "Pune"),
            Map.entry("noida", "Noida"),
            Map.entry("delhi", "Delhi"),
            Map.entry("new delhi", "Delhi"),
            Map.entry("ncr", "Delhi")
    );

    private static final Set<String> REMOTE_TOKENS = Set.of(
            "remote", "work from anywhere", "wfa", "wfh", "work from home",
            "anywhere", "fully remote", "remote india", "remote worldwide");

    private static final Set<String> HYBRID_TOKENS = Set.of(
            "hybrid", "flexible", "partially remote");

    private static final Set<String> ONSITE_TOKENS = Set.of(
            "onsite", "on-site", "in office", "in-office", "work from office",
            "wfo", "office", "on site");

    /** Normalize a raw city string to its canonical form (or trimmed title-ish input). */
    public String normalizeCity(String raw) {
        if (raw == null) return null;
        String key = raw.trim().toLowerCase(Locale.ROOT);
        if (key.isEmpty()) return null;
        return CITY_ALIASES.getOrDefault(key, raw.trim());
    }

    /**
     * Parse free-text into a {@link WorkMode}. Handles wordings from §12
     * ("Work from anywhere" → REMOTE, "3 days office" → HYBRID). Ambiguous → UNKNOWN.
     */
    public WorkMode parseWorkMode(String raw) {
        if (raw == null) return WorkMode.UNKNOWN;
        String t = raw.trim().toLowerCase(Locale.ROOT);
        if (t.isEmpty()) return WorkMode.UNKNOWN;

        // "3 days office" / "2 days in office" / "x days from office" → HYBRID
        if (t.matches(".*\\b\\d+\\s*days?\\b.*(office|onsite|on-site|wfo).*")
                || HYBRID_TOKENS.stream().anyMatch(t::contains)) {
            return WorkMode.HYBRID;
        }
        if (REMOTE_TOKENS.stream().anyMatch(t::contains)) return WorkMode.REMOTE;
        if (ONSITE_TOKENS.stream().anyMatch(t::contains)) return WorkMode.ONSITE;
        return WorkMode.UNKNOWN;
    }

    /**
     * Compare a job's location + work mode against the candidate's preferences.
     *
     * @param preferredCities   candidate's preferred cities (any spelling)
     * @param acceptsRemote     whether the candidate accepts remote work
     * @param openToRelocation  whether the candidate is open to relocating
     * @param jobCity           the job's city (any spelling; may be null)
     * @param jobWorkMode       the job's parsed work mode
     */
    public LocationMatch match(List<String> preferredCities,
                               boolean acceptsRemote,
                               boolean openToRelocation,
                               String jobCity,
                               WorkMode jobWorkMode) {

        if (jobWorkMode == WorkMode.REMOTE) {
            return acceptsRemote ? LocationMatch.REMOTE : LocationMatch.NONE;
        }

        String normJob = normalizeCity(jobCity);
        if (normJob == null) {
            // No city and not remote → cannot determine.
            return LocationMatch.UNKNOWN;
        }

        if (preferredCities != null) {
            for (String pref : preferredCities) {
                String normPref = normalizeCity(pref);
                if (normPref != null && normPref.equalsIgnoreCase(normJob)) {
                    // For a specific city preference, exact vs city are equivalent here.
                    return LocationMatch.CITY;
                }
            }
        }

        return openToRelocation ? LocationMatch.RELOCATION : LocationMatch.NONE;
    }
}

