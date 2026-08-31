package com.jobbot.module.discovery.adapter;

import com.jobbot.module.criteria.JobCriteria;
import com.jobbot.module.discovery.AtsType;
import com.jobbot.module.role.TargetRole;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * LinkedIn Jobs discovery adapter.
 *
 * <p>Uses LinkedIn's PUBLIC guest jobs endpoint (no login required):
 * {@code /jobs-guest/jobs/api/seeMoreJobPostings/search}. The endpoint returns
 * HTML fragments of job cards.
 *
 * <p>This adapter ONLY discovers jobs. Applications on LinkedIn are handled
 * exclusively by the Chrome Extension running inside the user's own browser
 * session — the server-side application-engine never touches LinkedIn.
 *
 * <p>Politeness rules:
 *  - Randomised 1.5–2.5 s sleep between pages
 *  - Max 2 pages (25 results each)
 *  - Honest User-Agent
 */
@Component
@Slf4j
public class LinkedInDiscoveryAdapter implements SearchBasedAdapter {

    private static final String BASE =
            "https://www.linkedin.com/jobs-guest/jobs/api/seeMoreJobPostings/search";
    private static final int MAX_PAGES = 2;
    private static final int PAGE_SIZE = 25;
    private static final int TIMEOUT_MS = 10_000;
    private static final String UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";

    @Override
    public AtsType type() {
        return AtsType.LINKEDIN;
    }

    @Override
    public List<DiscoveredPosting> discover(TargetRole role, JobCriteria criteria) {
        String keywords = role.getRoleTitle();
        List<String> locations = getLocations(role, criteria, List.of("India"));
        String experienceLevel = experienceLevelParam(role);
        List<DiscoveredPosting> out = new ArrayList<>();

        int locCount = 0;
        for (String location : locations) {
            if (locCount++ >= 3) break; // Limit to 3 locations

            for (int page = 0; page < MAX_PAGES; page++) {
                int start = page * PAGE_SIZE;
                String url = buildSearchUrl(keywords, location, experienceLevel, start);
                try {
                    Document doc = fetch(url);
                    List<DiscoveredPosting> cards = parse(doc);
                    out.addAll(cards);
                    if (cards.isEmpty()) break;
                    politeSleep();
                } catch (IOException e) {
                    log.warn("LinkedIn fetch failed for location '{}' at start={} : {}", location, start, e.getMessage());
                    break;
                }
            }
        }
        return out;
    }

    // -------- helpers --------

    Document fetch(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent(UA)
                .header("Accept-Language", "en-IN,en;q=0.9")
                .timeout(TIMEOUT_MS)
                .followRedirects(true)
                .ignoreHttpErrors(true)
                .get();
    }

    /** Public for unit-testing offline HTML fixtures. */
    public List<DiscoveredPosting> parse(Document doc) {
        List<DiscoveredPosting> out = new ArrayList<>();
        Elements cards = doc.select(
                "li div.base-card, li.result-card, div.base-search-card, "
                        + "div.job-search-card, div[data-entity-urn]");
        for (Element card : cards) {
            String urn = card.attr("data-entity-urn");
            String externalId = urnToId(urn);
            String title = firstText(card,
                    "h3.base-search-card__title", ".base-search-card__title",
                    "h3.result-card__title");
            String company = firstText(card,
                    "h4.base-search-card__subtitle a", "h4.base-search-card__subtitle",
                    ".base-search-card__subtitle");
            String location = firstText(card,
                    ".job-search-card__location", ".base-search-card__metadata .location");
            String jobUrl = firstAttr(card, "abs:href",
                    "a.base-card__full-link", "a.result-card__full-card-link", "a[href*='/jobs/view/']");
            if (externalId == null && jobUrl != null) {
                externalId = idFromUrl(jobUrl);
            }
            if (externalId == null || title == null) continue;

            out.add(new DiscoveredPosting(
                    AtsType.LINKEDIN,
                    "linkedin:" + externalId,
                    title,
                    company,
                    location,
                    jobUrl,
                    jobUrl,
                    null, // description not available via guest endpoint
                    null,
                    location,
                    null
            ));
        }
        return out;
    }

    String buildSearchUrl(String keywords, String location, String expLevel, int start) {
        StringBuilder sb = new StringBuilder(BASE).append("?");
        sb.append("keywords=").append(enc(keywords));
        if (location != null && !location.isBlank()) {
            sb.append("&location=").append(enc(location));
        } else {
            sb.append("&location=").append(enc("India"));
        }
        if (expLevel != null && !expLevel.isBlank()) {
            sb.append("&f_E=").append(expLevel);
        }
        sb.append("&start=").append(start);
        return sb.toString();
    }

    /** f_E: 1=internship, 2=entry, 3=associate, 4=mid-senior, 5=director, 6=exec */
    static String experienceLevelParam(TargetRole role) {
        Integer min = role.getMinimumExperience();
        Integer max = role.getMaximumExperience();
        int lo = min != null ? min : 0;
        int hi = max != null ? max : lo + 3;
        // 2-3 years user → mostly associate (3), some entry (2), some mid-senior (4)
        if (hi <= 1) return "1,2";
        if (hi <= 4) return "2,3";
        if (hi <= 7) return "3,4";
        return "4,5";
    }

    private List<String> getLocations(TargetRole role, JobCriteria c, List<String> def) {
        if (role.getLocations() != null && !role.getLocations().isEmpty()) {
            return role.getLocations();
        }
        if (c != null && c.getLocations() != null && !c.getLocations().isEmpty()) {
            return c.getLocations();
        }
        return def;
    }

    private static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    private static String urnToId(String urn) {
        if (urn == null || urn.isBlank()) return null;
        // e.g. "urn:li:jobPosting:3712345678"
        int idx = urn.lastIndexOf(':');
        return idx > 0 ? urn.substring(idx + 1) : null;
    }

    private static String idFromUrl(String url) {
        // /jobs/view/3712345678/?...
        String needle = "/jobs/view/";
        int i = url.indexOf(needle);
        if (i < 0) return null;
        String tail = url.substring(i + needle.length());
        int end = 0;
        while (end < tail.length() && Character.isDigit(tail.charAt(end))) end++;
        return end == 0 ? null : tail.substring(0, end);
    }

    private static String firstText(Element card, String... sels) {
        for (String sel : sels) {
            Element el = card.selectFirst(sel);
            if (el != null) {
                String t = el.text();
                if (t != null && !t.isBlank()) return t.trim();
            }
        }
        return null;
    }

    private static String firstAttr(Element card, String attr, String... sels) {
        for (String sel : sels) {
            Element el = card.selectFirst(sel);
            if (el != null) {
                String v = el.attr(attr);
                if (v != null && !v.isBlank()) return v.trim();
            }
        }
        return null;
    }

    private void politeSleep() {
        try {
            long ms = 1500L + (long) (Math.random() * 1000);
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}

