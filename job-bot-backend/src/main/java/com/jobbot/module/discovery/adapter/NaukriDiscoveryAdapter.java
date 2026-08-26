package com.jobbot.module.discovery.adapter;

import com.jobbot.module.criteria.JobCriteria;
import com.jobbot.module.discovery.AtsType;
import com.jobbot.module.role.TargetRole;
import com.microsoft.playwright.*;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Naukri.com search-based discovery adapter.
 *
 * <p>Naukri migrated to Next.js (CSR) in 2025 — plain HTTP fetchers (Jsoup) only
 * receive the loading shell. We use Playwright (headless Chromium) to execute
 * JavaScript and wait for the job cards to render before scraping the DOM.
 *
 * <p>Politeness rules:
 *  - Randomised 2–3 s sleep between page requests
 *  - Realistic User-Agent + webdriver masking
 *  - Hard cap of 3 pages per role
 */
@Component
@Slf4j
public class NaukriDiscoveryAdapter implements SearchBasedAdapter {

    private static final int MAX_PAGES = 3;
    private static final String UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";

    @Override
    public AtsType type() {
        return AtsType.NAUKRI;
    }

    @Override
    public List<DiscoveredPosting> discover(TargetRole role, JobCriteria criteria) {
        String location = firstLocation(role, criteria, "india");
        String roleSlug = hyphenate(role.getRoleTitle());
        List<DiscoveredPosting> out = new ArrayList<>();

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions()
                            .setHeadless(true)
                            .setArgs(List.of(
                                    "--no-sandbox",
                                    "--disable-dev-shm-usage",
                                    "--disable-blink-features=AutomationControlled")));

            BrowserContext context = browser.newContext(
                    new Browser.NewContextOptions()
                            .setUserAgent(UA)
                            .setViewportSize(1280, 900)
                            .setLocale("en-IN"));

            // Mask automation signals
            context.addInitScript(
                    "Object.defineProperty(navigator,'webdriver',{get:()=>undefined})");

            Page page = context.newPage();

            for (int pageNum = 1; pageNum <= MAX_PAGES; pageNum++) {
                String url = buildSearchUrl(roleSlug, location, role, pageNum);
                log.info("Naukri scan: fetching page {} → {}", pageNum, url);
                try {
                    page.navigate(url, new Page.NavigateOptions().setTimeout(30_000));

                    // Wait for job cards to render after Next.js hydration
                    page.waitForSelector(
                            "article.jobTuple, div.srp-jobtuple-wrapper, " +
                            "div[data-job-id], div.job-tuple-wrapper, " +
                            ".cust-job-tuple",
                            new Page.WaitForSelectorOptions().setTimeout(20_000));

                    String html = page.content();
                    Document doc = Jsoup.parse(html);
                    List<DiscoveredPosting> cards = parse(doc);
                    log.info("Naukri page {}: found {} jobs for role '{}'", pageNum, cards.size(), roleSlug);
                    out.addAll(cards);
                    if (cards.isEmpty()) break;
                    politeSleep();
                } catch (PlaywrightException e) {
                    log.warn("Naukri Playwright fetch failed for '{}' page {}: {}", roleSlug, pageNum, e.getMessage());
                    break;
                }
            }

            browser.close();
        } catch (Exception e) {
            log.error("Naukri discovery error for role '{}': {}", roleSlug, e.getMessage());
        }
        return out;
    }

    /** Public for unit-testing with offline HTML fixtures. */
    public List<DiscoveredPosting> parse(Document doc) {
        List<DiscoveredPosting> out = new ArrayList<>();
        // Naukri Next.js 2025/2026 — multiple fallback selectors for resilience
        Elements cards = doc.select(
                "article.jobTuple, " +
                "div.srp-jobtuple-wrapper, " +
                "div[data-job-id], " +
                "div.job-tuple-wrapper, " +
                ".cust-job-tuple");
        log.debug("Naukri parse: found {} raw cards", cards.size());

        for (Element card : cards) {
            String title = firstText(card,
                    "a.title", "a.row1", ".jobTitle a",
                    "a[title]", "h2 a", ".designation a");
            String company = firstText(card,
                    "a.subTitle", ".companyName a", ".comp-name",
                    "a.comp-name", ".company-name a");
            String location = firstText(card,
                    ".locWdth span", ".location span", "[data-city]",
                    ".loc-wrap span", ".ni-job-tuple-icon-srp-location span");
            String experience = firstText(card,
                    ".expwdth", ".exp span", "[data-requiredexp]", ".experience span");
            String salary = firstText(card,
                    ".salary", "[data-salary]", ".sal-wrap span");

            String jobUrl = firstAttr(card, "abs:href",
                    "a.title", "a.row1", ".jobTitle a", "a[title]", "h2 a");
            if (jobUrl == null || jobUrl.isBlank()) {
                jobUrl = card.attr("abs:href");
            }
            String externalId = extractExternalId(jobUrl, card);
            if (externalId == null || title == null) {
                log.trace("Naukri: skipping card — title={} externalId={}", title, externalId);
                continue;
            }

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

    private void politeSleep() {
        try {
            long ms = 2000L + (long) (Math.random() * 1000);
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
