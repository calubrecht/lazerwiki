package us.calubrecht.lazerwiki.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.model.Site;
import us.calubrecht.lazerwiki.repository.SiteRepository;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = SiteService.class)
@ActiveProfiles("test")
public class SiteServiceTest {

    @Autowired
    SiteService underTest;

    @MockBean
    SiteRepository repository;

    @Test
    public void test() {
        Site defaultSite = new Site();
        defaultSite.name = "normal";
        defaultSite.hostname = "*";
        Site site1 = new Site();
        site1.name = "site1";
        site1.hostname = "site1.org";
        when(repository.findByHostname(eq("*"))).thenReturn(defaultSite);
        when(repository.findByHostname(eq("site1.org"))).thenReturn(site1);
        assertEquals("normal", underTest.getSiteForHostname("anyHost"));
        assertEquals("site1", underTest.getSiteForHostname("site1.org"));
    }

    @Test
    public void test_getSiteNameForHostname() {
        Site site1 = new Site();
        site1.name = "site1";
        site1.hostname = "site1.org";
        site1.siteName = "The Test Wiki";
        Site otherSite = new Site();
        otherSite.name = "otherSite";
        otherSite.hostname = "otherSite.org";
        when(repository.findByHostname(eq("site1.org"))).thenReturn(site1);
        when(repository.findByHostname(eq("othersite.org"))).thenReturn(otherSite);

        assertEquals("The Test Wiki", underTest.getSiteNameForHostname("site1.org"));
        assertEquals("Lazerwiki", underTest.getSiteNameForHostname("othersite.org"));
        assertEquals("Lazerwiki", underTest.getSiteNameForHostname("missingSite.org"));
    }

    /**
     *
     *   public Object getSettingForHostname(String hostname, String setting) {
     *         Site s =  siteRepository.findByHostname(hostname.toLowerCase());
     *         if (s == null || !s.settings.containsValue(setting)) {
     *             s = siteRepository.findByHostname("*");
     *         }
     *         if (s == null ) {
     *
     *             return null;
     *         }
     *         return s.settings.get(setting);
     *     }
     */
    @Test
    public void test_getSettingForHostname() {
        Site noSettingSite = new Site();
        noSettingSite.settings = Map.of();
        Site defSite = new Site();
        defSite.settings = Map.of("setting1", "value");
        Site settingSite = new Site();
        settingSite.settings = Map.of("setting1", "othervalue");
        when(repository.findByHostname("site1")).thenReturn(noSettingSite);
        when(repository.findByHostname("site2")).thenReturn(settingSite);
        when(repository.findByHostname("*")).thenReturn(defSite);

        assertEquals("value", underTest.getSettingForHostname("site1", "setting1"));
        assertEquals("othervalue", underTest.getSettingForHostname("site2", "setting1"));
    }
    @Test
    public void test_getSettingForHostnameNoSettings() {
        assertEquals(null, underTest.getSettingForHostname("site1", "setting1"));
    }

    @Test
    void getHostForSitename() {
        Site blue = new Site();
        blue.hostname = "water.com";
        when(repository.findById("blue")).thenReturn(Optional.of(blue));
        assertEquals("water.com", underTest.getHostForSitename("blue"));
    }
}
