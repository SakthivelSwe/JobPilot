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
 * Naukri.com search-based discovery adapter.
 *
 * <p>Uses Jsoup to fetch the public search-results HTML page (no auth). Card
 * selectors are Naukri's public 2026 layout (also fall back to earlier selectors
 * for robustness).
 *
 * <p>Politeness rules baked in:
 *  - Randomised 2–3.5 s sleep between page requests
 *  - Honest User-Agent (no evasion)
 *  - 10 s connect + read timeout
 *  - Hard cap of 3 pages per role
 */
@Component
@Slf4j
public class NaukriDiscoveryAdapter implements SearchBasedAdapter {

    private static final int MAX_PAGES = 3;
    private static final int TIMEOUT_MS = 10_000;
    private static final String UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";

    @Override
    public AtsType type() {
        return AtsType.NAUKRI;
    }

    @Override
    public List<DiscoveredPosting> discover(TargetRole role, JobCriteria criteria) {
        String location = firstLocation(role, criteria, "india");
        String roleSlug = hyphenate(role.getRoleTitle());
        List<DiscoveredPosting> out = new ArrayList<>();

        for (int page = 1; page <= MAX_PAGES; page++) {
            String url = buildSearchUrl(roleSlug, location, role, page);
            try {
                Document doc = fetch(url);
                List<DiscoveredPosting> cards = parse(doc);
                out.addAll(cards);
                if (cards.isEmpty()) break; // no more pages
                politeSleep(page);
            } catch (IOException e) {
                log.warn("Naukri fetch failed for '{}' page {}: {}", roleSlug, page, e.getMessage());
                break;
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
        Elements cards = doc.select("article.jobTuple, div.srp-jobtuple-wrapper, div[data-job-id]");
        for (Element card : cards) {
            String title = firstText(card,
                    "a.title", "h2.title a", ".jobTitle a", "a[data-job-title]");
            String company = firstText(card,
                    "a.subTitle", ".companyName a", ".subTitle a", "a.comp-name");
            String location = firstText(card,
                    ".locWdth span", ".job-details-others .location", "[data-city]");
            String experience = firstText(card,
                    ".expwdth", ".exp span", "[data-requiredexp]");
            String salary = firstText(card,
                    ".salary", "[data-salary]", ".salary span");
            String jobUrl = firstAttr(card, "abs:href",
                    "a.title", "h2.title a", ".jobTitle a");
            String externalId = extractExternalId(jobUrl, card);
            if (externalId == null || title == null) continue;

            String description = joinNonBlank(experience, salary);
            out.add(new DiscoveredPosting(
                    AtsType.NAUKRI,
                    externalId,
                    title,
                    company,
                    location,
                    jobUrl,
                    jobUrl,
                    description,
                    null,
                    location,
                    null
            ));
        }
        return out;
    }

    String buildSearchUrl(String roleSlug, String locationSlug, TargetRole role, int page) {
        StringBuilder sb = new StringBuilder("https://www.naukri.com/");
        sb.append(URLEncoder.encode(roleSlug, StandardCharsets.UTF_8));
        sb.append("-jobs");
        if (locationSlug != null && !locationSlug.isBlank()
                && !"remote".equals(locationSlug)) {
            sb.append("-in-").append(URLEncoder.encode(locationSlug, StandardCharsets.UTF_8));
        }
        if (page > 1) sb.append("-").append(page);
        sb.append("?");
        if ("remote".equals(locationSlug)) {
            sb.append("jobtype=remote&");
        }
        if (role.getMinimumExperience() != null || role.getMaximumExperience() != null) {
            int min = role.getMinimumExperience() != null ? role.getMinimumExperience() : 0;
            int max = role.getMaximumExperience() != null ? role.getMaximumExperience() : min + 3;
            sb.append("experience=").append(min).append("-").append(max);
        }
        String s = sb.toString();
        return s.endsWith("?") ? s.substring(0, s.length() - 1) : s;
    }

    private String firstLocation(TargetRole role, JobCriteria c, String def) {
        if (role.getLocations() != null && !role.getLocations().isEmpty()) {
            return hyphenate(role.getLocations().get(0));
        }
        if (c != null && c.getLocations() != null && !c.getLocations().isEmpty()) {
            return hyphenate(c.getLocations().get(0));
        }
        return def;
    }

    static String hyphenate(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "-");
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

    private static String extractExternalId(String url, Element card) {
        String dataId = card.attr("data-job-id");
        if (dataId != null && !dataId.isBlank()) return "naukri:" + dataId;
        if (url == null) return null;
        int idx = url.lastIndexOf('-');
        if (idx < 0) return null;
        String tail = url.substring(idx + 1);
        // e.g. "software-engineer-abc-123456789?..." → 123456789
        int q = tail.indexOf('?');
        if (q >= 0) tail = tail.substring(0, q);
        return tail.isBlank() ? null : "naukri:" + tail;
    }

    private static String joinNonBlank(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p != null && !p.isBlank()) {
                if (sb.length() > 0) sb.append(" · ");
                sb.append(p.trim());
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private void politeSleep(int page) {
        try {
            long ms = 2000L + (long) (Math.random() * 1500);
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}

