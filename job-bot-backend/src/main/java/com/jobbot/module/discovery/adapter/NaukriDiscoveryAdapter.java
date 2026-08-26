package com.jobbot.module.discovery.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobbot.module.criteria.JobCriteria;
import com.jobbot.module.discovery.AtsType;
import com.jobbot.module.platform.PlatformSessionService;
import com.jobbot.module.role.TargetRole;
import com.jobbot.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Naukri.com search-based discovery adapter.
 *
 * <p>Strategy (two-tier):
 * <ol>
 *   <li><b>API-first</b>: Hit Naukri's internal search API with the user's authenticated
 *       session cookies — bypasses recaptcha. Returns JSON directly.</li>
 *   <li><b>HTML fallback</b>: Jsoup fetch of the HTML search page (works when Naukri serves SSR).</li>
 * </ol>
 */
@Component
@Slf4j
public class NaukriDiscoveryAdapter implements SearchBasedAdapter {

    private static final int MAX_PAGES = 3;
    private static final String UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";

    private static final String API_URL =
            "https://www.naukri.com/jobapi/v3/search?noOfResults=20&urlType=search_by_key_loc" +
            "&searchType=adv&src=jobsearchDesk&pageNo=%d&keyword=%s&location=%s" +
            "&experience=%d&experienceDD=%d&seoKey=%s";

    private final PlatformSessionService sessionService;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public NaukriDiscoveryAdapter(PlatformSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public AtsType type() {
        return AtsType.NAUKRI;
    }

    @Override
    public List<DiscoveredPosting> discover(TargetRole role, JobCriteria criteria) {
        String location = firstLocation(role, criteria, "india");
        String keyword  = role.getRoleTitle();
        int expMin = role.getMinimumExperience() != null ? role.getMinimumExperience() : 0;
        int expMax = role.getMaximumExperience() != null ? role.getMaximumExperience() : expMin + 3;
        String roleSlug = hyphenate(keyword);

        // Load the user's Naukri session cookie for authenticated requests
        String sessionCookie = null;
        try {
            String userId = SecurityUtils.getCurrentUserId();
            sessionCookie = sessionService.loadSessionCookieString("NAUKRI", userId);
            if (sessionCookie != null) {
                log.info("Naukri scan: using authenticated session for userId={}", userId);
            } else {
                log.info("Naukri scan: no session cookie found, proceeding unauthenticated");
            }
        } catch (Exception e) {
            log.debug("Could not load Naukri session: {}", e.getMessage());
        }

        List<DiscoveredPosting> out = new ArrayList<>();
        for (int page = 1; page <= MAX_PAGES; page++) {
            List<DiscoveredPosting> pageResults = tryApiPage(keyword, location, expMin, expMax, roleSlug, page, sessionCookie);
            if (pageResults.isEmpty() && page == 1) {
                log.warn("Naukri API returned 0 for '{}', trying HTML fallback", keyword);
                pageResults = tryHtmlFallback(roleSlug, location, role, page, sessionCookie);
            }
            log.info("Naukri page {}: {} jobs for role '{}'", page, pageResults.size(), keyword);
            out.addAll(pageResults);
            if (pageResults.isEmpty()) break;
            politeSleep();
        }
        return out;
    }

    // ─── TIER 1: JSON API ────────────────────────────────────────────────────

    private List<DiscoveredPosting> tryApiPage(
            String keyword, String location, int expMin, int expMax,
            String roleSlug, int page, String sessionCookie) {

        List<DiscoveredPosting> out = new ArrayList<>();
        try {
            String encKeyword  = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String encLocation = URLEncoder.encode(location, StandardCharsets.UTF_8);
            String seoKey      = roleSlug + "-jobs-in-" + hyphenate(location);
            String url = String.format(API_URL, page, encKeyword, encLocation,
                    expMin, expMax, seoKey);
            log.debug("Naukri API URL: {}", url);

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", UA)
                    .header("Accept", "application/json")
                    .header("Referer", "https://www.naukri.com/")
                    .header("appid", "109")
                    .header("systemid", "109")
                    .timeout(Duration.ofSeconds(20))
                    .GET();

            // Attach session cookie if available — bypasses recaptcha
            if (sessionCookie != null && !sessionCookie.isBlank()) {
                reqBuilder.header("Cookie", sessionCookie);
            }

            HttpResponse<String> resp = http.send(reqBuilder.build(),
                    HttpResponse.BodyHandlers.ofString());
            log.info("Naukri API status={} page={} keyword='{}'", resp.statusCode(), page, keyword);

            if (resp.statusCode() == 406) {
                log.warn("Naukri API recaptcha required (406) — session cookie may be expired");
                return out;
            }
            if (resp.statusCode() != 200) {
                log.warn("Naukri API HTTP {} for keyword='{}' page={}", resp.statusCode(), keyword, page);
                return out;
            }

            JsonNode root = mapper.readTree(resp.body());
            JsonNode jobs = root.path("jobDetails");
            if (!jobs.isArray() || jobs.isEmpty()) {
                jobs = root.path("results").path("jobDetails");
            }

            for (JsonNode job : jobs) {
                String title   = text(job, "title", "jobTitle");
                String company = text(job, "companyName", "company");
                String loc     = text(job, "ugcCity", "jobLocation");
                // location often nested inside placeholders array
                if ((loc == null || loc.isBlank()) && job.path("placeholders").isArray()) {
                    for (JsonNode ph : job.path("placeholders")) {
                        if ("location".equals(ph.path("type").asText())) {
                            loc = ph.path("label").asText(null);
                            break;
                        }
                    }
                }
                String jobUrl = text(job, "jdURL", "jobUrl", "jdUrl");
                String extId  = text(job, "jobId");
                if (extId != null) extId = "naukri:" + extId;
                String exp = text(job, "experienceText", "experience");
                String sal = text(job, "salaryDetail", "salary");

                if (title == null || extId == null) continue;
                out.add(new DiscoveredPosting(
                        AtsType.NAUKRI, extId, title, company, loc,
                        jobUrl, jobUrl, joinNonBlank(exp, sal), null, loc, null));
            }
        } catch (Exception e) {
            log.warn("Naukri API call failed for '{}' page {}: {}", keyword, page, e.getMessage());
        }
        return out;
    }

    // ─── TIER 2: HTML fallback ───────────────────────────────────────────────

    private List<DiscoveredPosting> tryHtmlFallback(
            String roleSlug, String location, TargetRole role, int page, String sessionCookie) {
        List<DiscoveredPosting> out = new ArrayList<>();
        String url = buildSearchUrl(roleSlug, location, role, page);
        try {
            var conn = Jsoup.connect(url)
                    .userAgent(UA)
                    .header("Accept-Language", "en-IN,en;q=0.9")
                    .timeout(15_000)
                    .followRedirects(true)
                    .ignoreHttpErrors(true);
            if (sessionCookie != null && !sessionCookie.isBlank()) {
                conn.header("Cookie", sessionCookie);
            }
            Document doc = conn.get();
            out.addAll(parseHtml(doc));
        } catch (IOException e) {
            log.warn("Naukri HTML fallback failed for '{}' page {}: {}", roleSlug, page, e.getMessage());
        }
        return out;
    }

    /** Public for unit-testing with offline HTML fixtures. */
    public List<DiscoveredPosting> parseHtml(Document doc) {
        List<DiscoveredPosting> out = new ArrayList<>();
        Elements cards = doc.select(
                "article.jobTuple, div.srp-jobtuple-wrapper, " +
                "div[data-job-id], div.job-tuple-wrapper, " +
                ".cust-job-tuple, [class*='jobTuple'], [class*='job-tuple']");
        for (Element card : cards) {
            String title   = firstText(card, "a.title", "a.row1", ".jobTitle a",
                    "a[title]", "h2 a", ".designation a", "[class*='title'] a");
            String company = firstText(card, "a.subTitle", ".companyName a", ".comp-name",
                    "a.comp-name", "[class*='company'] a");
            String loc     = firstText(card, ".locWdth span", ".location span",
                    "[data-city]", ".loc-wrap span", "span[class*='location']");
            String exp     = firstText(card, ".expwdth", ".exp span",
                    "[data-requiredexp]", "span[class*='exp']");
            String sal     = firstText(card, ".salary", "[data-salary]", "span[class*='salary']");
            String jobUrl  = firstAttr(card, "abs:href",
                    "a.title", "a.row1", ".jobTitle a", "a[title]", "h2 a");
            String extId   = extractExternalId(jobUrl, card);
            if (extId == null || title == null) continue;
            out.add(new DiscoveredPosting(AtsType.NAUKRI, extId, title, company, loc,
                    jobUrl, jobUrl, joinNonBlank(exp, sal), null, loc, null));
        }
        return out;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    String buildSearchUrl(String roleSlug, String locationSlug, TargetRole role, int page) {
        StringBuilder sb = new StringBuilder("https://www.naukri.com/");
        sb.append(URLEncoder.encode(roleSlug, StandardCharsets.UTF_8)).append("-jobs");
        if (locationSlug != null && !locationSlug.isBlank() && !"remote".equals(locationSlug)) {
            sb.append("-in-").append(URLEncoder.encode(locationSlug, StandardCharsets.UTF_8));
        }
        if (page > 1) sb.append("-").append(page);
        sb.append("?");
        if ("remote".equals(locationSlug)) sb.append("jobtype=remote&");
        if (role.getMinimumExperience() != null || role.getMaximumExperience() != null) {
            int min = role.getMinimumExperience() != null ? role.getMinimumExperience() : 0;
            int max = role.getMaximumExperience() != null ? role.getMaximumExperience() : min + 3;
            sb.append("experience=").append(min).append("-").append(max);
        }
        String s = sb.toString();
        return s.endsWith("?") ? s.substring(0, s.length() - 1) : s;
    }

    private String firstLocation(TargetRole role, JobCriteria c, String def) {
        if (role.getLocations() != null && !role.getLocations().isEmpty())
            return role.getLocations().get(0);
        if (c != null && c.getLocations() != null && !c.getLocations().isEmpty())
            return c.getLocations().get(0);
        return def;
    }

    static String hyphenate(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "-");
    }

    private static String text(JsonNode node, String... fields) {
        for (String f : fields) {
            JsonNode n = node.path(f);
            if (n.isTextual() && !n.asText().isBlank()) return n.asText().trim();
        }
        return null;
    }

    private static String firstText(Element card, String... sels) {
        for (String sel : sels) {
            Element el = card.selectFirst(sel);
            if (el != null) { String t = el.text(); if (t != null && !t.isBlank()) return t.trim(); }
        }
        return null;
    }

    private static String firstAttr(Element card, String attr, String... sels) {
        for (String sel : sels) {
            Element el = card.selectFirst(sel);
            if (el != null) { String v = el.attr(attr); if (v != null && !v.isBlank()) return v.trim(); }
        }
        return null;
    }

    private static String extractExternalId(String url, Element card) {
        String dataId = card.attr("data-job-id");
        if (dataId != null && !dataId.isBlank()) return "naukri:" + dataId;
        if (url == null) return null;
        int idx = url.lastIndexOf('-'); if (idx < 0) return null;
        String tail = url.substring(idx + 1);
        int q = tail.indexOf('?'); if (q >= 0) tail = tail.substring(0, q);
        return tail.isBlank() ? null : "naukri:" + tail;
    }

    private static String joinNonBlank(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p != null && !p.isBlank()) { if (sb.length() > 0) sb.append(" · "); sb.append(p.trim()); }
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private void politeSleep() {
        try { Thread.sleep(2000L + (long)(Math.random() * 1000)); }
        catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }
}
