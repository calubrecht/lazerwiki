package us.calubrecht.lazerwiki.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.model.ImageRef;
import us.calubrecht.lazerwiki.model.MediaOverride;
import us.calubrecht.lazerwiki.repository.ImageRefRepository;
import us.calubrecht.lazerwiki.repository.MediaOverrideRepository;
import us.calubrecht.lazerwiki.util.ImageUtil;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {MediaOverrideService.class})
@ActiveProfiles("test")
class MediaOverrideServiceTest {

    @Autowired
    MediaOverrideService underTest;

    @MockBean
    SiteService siteService;

    @MockBean
    MediaOverrideRepository repo;

    @MockBean
    ImageRefRepository imageRefRepository;

    @BeforeEach
    void init() {
        when(siteService.getSiteForHostname("localhost")).thenReturn("default");
    }

    @Test
    void getOverrides() {
        MediaOverride override = new MediaOverride("site", "ns1", "page1", "ns1", "img1.jpg", "ns1", "img2.jpg");
        when(repo.findAllBySiteAndSourcePageNSAndSourcePageNameOrderById("default", "ns1", "page1")).
               thenReturn(List.of(override));

        List<MediaOverride> overrides = underTest.getOverrides("localhost", "ns1:page1");
        assertEquals(1, overrides.size());
    }

    @Test
    void getOverridesForImage() {
        MediaOverride override = new MediaOverride("site", "ns1", "page1", "ns1", "img1.jpg", "ns1", "img2.jpg");
        when(repo.findAllBySiteAndNewTargetFileNSAndNewTargetFileName("default", "ns1", "img1.jpg")).
                thenReturn(List.of(override));

        List<MediaOverride> overrides = underTest.getOverridesForImage("localhost", "ns1:img1.jpg");
        assertEquals(1, overrides.size());
    }

    @Test
    void createOverride() {
        List<ImageRef> links = List.of(new ImageRef("default", "ns1", "page1", "ns1", "img1.jpg"));
        when(imageRefRepository.findAllBySiteAndImageNSAndImageRef("default", "ns1", "img1.jpg")).thenReturn(links);
        List<MediaOverride> existingOverrides = List.of(new MediaOverride("default", "ns1", "page5", "ns", "img.jpg", "ns1", "img1.jpg"));
        when(repo.findAllBySiteAndNewTargetFileNSAndNewTargetFileName("default", "ns1", "img1.jpg")).thenReturn(existingOverrides);
        when(imageRefRepository.findAllBySiteAndImageNSAndImageRef("default", "ns1", "img1.jpg")).thenReturn(links);
        underTest.createOverride("localhost", "ns1", "img1.jpg", "ns2", "img2.jpg");

        verify(repo).deleteBySiteAndNewTargetFileNSAndNewTargetFileName("default", "ns1", "img1.jpg");
        ArgumentCaptor<List<MediaOverride>> overrides = ArgumentCaptor.forClass(List.class);
        verify(repo, times(2)).saveAll(overrides.capture());
        // 1 direct overrides
        assertEquals(1, overrides.getAllValues().get(0).size());
        assertEquals("ns1:page1", overrides.getAllValues().get(0).get(0).getSource());
        // existing overrides
        assertEquals(1, overrides.getAllValues().get(1).size());
        assertEquals("ns1:page5", overrides.getAllValues().get(1).get(0).getSource());
    }

    @Test
    void moveOverrides() {
        List<MediaOverride> existingOverrides = List.of(new MediaOverride("default", "ns1", "page1", "ns", "img.jpg", "ns1", "img1.jpg"));
        when(repo.findAllBySiteAndSourcePageNSAndSourcePageNameOrderById("default", "ns1", "page1")).thenReturn(existingOverrides);
        underTest.moveOverrides("localhost", "ns1:page1", "ns2:page2");

        verify(repo).deleteBySiteAndSourcePageNSAndSourcePageName("default", "ns1", "page1");
        List<MediaOverride> newOverrides = List.of(new MediaOverride("default", "ns2", "page2", "ns", "img.jpg", "ns1", "img1.jpg"));
        verify(repo).saveAll(newOverrides);
    }

    @Test
    void deleteOverrides() {
        underTest.deleteOverrides("localhost", "ns1:page1");

        verify(repo).deleteBySiteAndSourcePageNSAndSourcePageName("default", "ns1", "page1");
    }
}