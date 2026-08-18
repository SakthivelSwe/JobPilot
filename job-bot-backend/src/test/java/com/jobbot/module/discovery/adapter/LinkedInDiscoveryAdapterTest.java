package com.jobbot.module.discovery.adapter;

import com.jobbot.module.role.TargetRole;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LinkedInDiscoveryAdapterTest {

    private final LinkedInDiscoveryAdapter adapter = new LinkedInDiscoveryAdapter();

    @Test
    void parsesGuestSearchCards() {
        String html = """
                <ul>
                  <li>
                    <div class="base-search-card" data-entity-urn="urn:li:jobPosting:3712345678">
                      <a class="base-card__full-link" href="https://www.linkedin.com/jobs/view/3712345678/?trk=x">
                        <h3 class="base-search-card__title">Senior Java Engineer</h3>
                      </a>
                      <h4 class="base-search-card__subtitle"><a>InfoTech Ltd</a></h4>
                      <span class="job-search-card__location">Chennai, Tamil Nadu, India</span>
                    </div>
                  </li>
                  <li>
                    <div class="base-search-card" data-entity-urn="urn:li:jobPosting:3712345679">
                      <a class="base-card__full-link" href="https://www.linkedin.com/jobs/view/3712345679/">
                        <h3 class="base-search-card__title">Spring Boot Developer</h3>
                      </a>
                      <h4 class="base-search-card__subtitle">Fintech Corp</h4>
                      <span class="job-search-card__location">Bengaluru, Karnataka</span>
                    </div>
                  </li>
                </ul>
                """;
        List<DiscoveredPosting> out = adapter.parse(Jsoup.parse(html, "https://www.linkedin.com/"));
        assertEquals(2, out.size());
        DiscoveredPosting first = out.get(0);
        assertEquals("Senior Java Engineer", first.title());
        assertEquals("InfoTech Ltd", first.company());
        assertEquals("linkedin:3712345678", first.externalId());
        assertTrue(first.location().contains("Chennai"));
        assertTrue(first.jobUrl().contains("3712345678"));
        assertNull(first.description()); // guest endpoint gives no JD
    }

    @Test
    void experienceLevelMapsToCorrectLinkedInFilter() {
        TargetRole entry = TargetRole.builder().maximumExperience(1).build();
        assertEquals("1,2", LinkedInDiscoveryAdapter.experienceLevelParam(entry));

        TargetRole associate = TargetRole.builder().minimumExperience(2).maximumExperience(4).build();
        assertEquals("2,3", LinkedInDiscoveryAdapter.experienceLevelParam(associate));

        TargetRole midSenior = TargetRole.builder().minimumExperience(5).maximumExperience(7).build();
        assertEquals("3,4", LinkedInDiscoveryAdapter.experienceLevelParam(midSenior));

        TargetRole director = TargetRole.builder().minimumExperience(10).maximumExperience(15).build();
        assertEquals("4,5", LinkedInDiscoveryAdapter.experienceLevelParam(director));
    }

    @Test
    void searchUrlIncludesKeywordsLocationAndExperience() {
        String url = adapter.buildSearchUrl("Java Backend", "Chennai", "2,3", 25);
        assertTrue(url.contains("keywords=Java+Backend"), url);
        assertTrue(url.contains("location=Chennai"), url);
        assertTrue(url.contains("f_E=2,3"), url);
        assertTrue(url.contains("start=25"), url);
    }

    @Test
    void skipsCardsWithoutTitle() {
        String html = """
                <div class="base-search-card" data-entity-urn="urn:li:jobPosting:1">
                  <a class="base-card__full-link" href="https://www.linkedin.com/jobs/view/1/"></a>
                </div>
                """;
        List<DiscoveredPosting> out = adapter.parse(Jsoup.parse(html, "https://www.linkedin.com/"));
        assertTrue(out.isEmpty());
    }
}

