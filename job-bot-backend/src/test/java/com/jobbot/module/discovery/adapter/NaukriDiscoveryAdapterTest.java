package com.jobbot.module.discovery.adapter;

import com.jobbot.module.criteria.JobCriteria;
import com.jobbot.module.role.TargetRole;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NaukriDiscoveryAdapterTest {

    private final NaukriDiscoveryAdapter adapter = new NaukriDiscoveryAdapter();

    @Test
    void parsesJobTupleCards() {
        String html = """
                <html><body>
                <article class="jobTuple" data-job-id="9876543">
                  <h2 class="title"><a href="https://www.naukri.com/job-listings-java-backend-engineer-acme-9876543">Java Backend Engineer</a></h2>
                  <a class="subTitle">Acme Corp</a>
                  <span class="expwdth">2-4 Yrs</span>
                  <span class="salary">₹8-14 LPA</span>
                  <span class="locWdth"><span>Chennai, Bangalore</span></span>
                </article>
                <article class="jobTuple" data-job-id="9876544">
                  <a class="title" href="/job-listings-spring-boot-developer-9876544">Spring Boot Developer</a>
                  <a class="subTitle">Beta Systems</a>
                  <span class="expwdth">3-5 Yrs</span>
                  <span class="locWdth"><span>Remote</span></span>
                </article>
                </body></html>
                """;
        List<DiscoveredPosting> out = adapter.parse(Jsoup.parse(html, "https://www.naukri.com/"));
        assertEquals(2, out.size());
        DiscoveredPosting first = out.get(0);
        assertEquals("Java Backend Engineer", first.title());
        assertEquals("Acme Corp", first.company());
        assertTrue(first.location().contains("Chennai"));
        assertEquals("naukri:9876543", first.externalId());
        assertNotNull(first.jobUrl());
    }

    @Test
    void skipsCardsWithoutTitle() {
        String html = """
                <html><body>
                <article class="jobTuple" data-job-id="1">
                  <a class="subTitle">No Title Corp</a>
                </article>
                </body></html>
                """;
        List<DiscoveredPosting> out = adapter.parse(Jsoup.parse(html, "https://www.naukri.com/"));
        assertTrue(out.isEmpty());
    }

    @Test
    void buildsSearchUrlWithHyphenatedRoleAndCity() {
        TargetRole role = TargetRole.builder()
                .roleTitle("Java Backend Developer")
                .minimumExperience(2).maximumExperience(4)
                .build();
        String url = adapter.buildSearchUrl(
                NaukriDiscoveryAdapter.hyphenate(role.getRoleTitle()),
                NaukriDiscoveryAdapter.hyphenate("Chennai"),
                role, 1);
        assertTrue(url.startsWith("https://www.naukri.com/"));
        assertTrue(url.contains("java-backend-developer-jobs-in-chennai"));
        assertTrue(url.contains("experience=2-4"));
    }

    @Test
    void hyphenateStripsPunctuationAndLowercases() {
        // Hyphens are treated as punctuation (removed), then spaces become hyphens.
        assertEquals("java-fullstack-developer",
                NaukriDiscoveryAdapter.hyphenate("Java, Full-Stack Developer"));
        assertEquals("java-backend-developer",
                NaukriDiscoveryAdapter.hyphenate("Java Backend Developer"));
    }

    @Test
    void buildsRemoteUrlWhenLocationIsRemote() {
        TargetRole role = TargetRole.builder().roleTitle("java").build();
        String url = adapter.buildSearchUrl("java-jobs", "remote", role, 1);
        assertTrue(url.contains("jobtype=remote"), url);
        assertFalse(url.contains("-in-remote"), url);
    }

    // Simple criteria constructor for API completeness (unused fields).
    @SuppressWarnings("unused")
    private JobCriteria emptyCriteria() { return JobCriteria.builder().name("x").build(); }
}

