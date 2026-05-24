package us.calubrecht.lazerwiki.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.model.Site;
import us.calubrecht.lazerwiki.repository.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = SiteDelService.class)
@ActiveProfiles("test")
public class SiteDelServiceTest {

    @Autowired
    SiteDelService underTest;
    @MockitoBean
    SiteRepository siteRepository;
    @MockitoBean
    MediaRecordRepository mediaRecordRepository;
    @MockitoBean
    MediaHistoryRepository mediaHistoryRepository;
    @MockitoBean
    ImageRefRepository imageRefRepository;
    @MockitoBean
    LinkRepository linkRepository;
    @MockitoBean
    NamespaceRepository namespaceRepository;
    @MockitoBean
    PageRepository pageRepository;
    @MockitoBean
    PageCacheRepository pageCacheRepository;
    @MockitoBean
    PageLockRepository pageLockRepository;

    @Value("${lazerwiki.static.file.root}")
    String staticFileRoot;

    @BeforeEach
    public void setup() {
        underTest.staticFileRoot = staticFileRoot;
    }


    @Test
    void delSite() throws IOException {
        when(siteRepository.findBySiteName("site1")).thenReturn(new Site("existingSite", "", "site1"));
        if (!Paths.get(staticFileRoot, "existingSite", "media").toFile().mkdirs()) {
            throw new IOException("Test broken");
        }
        File f = Paths.get(staticFileRoot, "existingSite", "media", "test.write").toFile();
        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write(1);
        }
        assertTrue(f.exists());

        assertFalse(underTest.deleteSiteCompletely("nonExistingSite", "Bob"));

        String site = "existingSite";

        assertTrue(underTest.deleteSiteCompletely("site1", "Bob"));

        assertFalse(f.exists());
        assertFalse(Paths.get(staticFileRoot, site).toFile().exists());

        verify(mediaRecordRepository).deleteBySite(site);
        verify(mediaHistoryRepository).deleteBySite(site);
        verify(imageRefRepository).deleteBySite(site);
        verify(linkRepository).deleteBySite(site);
        verify(namespaceRepository).deleteBySite(site);
        verify(pageRepository).deleteBySite(site);
        verify(pageCacheRepository).deleteBySite(site);
        verify(pageLockRepository).deleteBySite(site);
        verify(siteRepository).deleteById(site);
    }
}
