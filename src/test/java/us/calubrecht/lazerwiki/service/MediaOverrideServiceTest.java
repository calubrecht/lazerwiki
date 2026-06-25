package us.calubrecht.lazerwiki.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import us.calubrecht.lazerwiki.model.ImageRef;
import us.calubrecht.lazerwiki.model.MediaOverride;
import us.calubrecht.lazerwiki.repository.ImageRefRepository;
import us.calubrecht.lazerwiki.repository.MediaOverrideRepository;

@SpringBootTest(classes = {MediaOverrideService.class})
@ActiveProfiles("test")
@SuppressWarnings("unchecked")
class MediaOverrideServiceTest {

  @Autowired MediaOverrideService underTest;

  @MockitoBean MediaOverrideRepository repo;

  @MockitoBean ImageRefRepository imageRefRepository;

  @Test
  void test_getOverrides() {
    MediaOverride override =
        new MediaOverride("site", "ns1", "page1", "ns1", "img1.jpg", "ns1", "img2.jpg");
    when(repo.findAllBySiteAndSourcePageNSAndSourcePageNameOrderById("default", "ns1", "page1"))
        .thenReturn(List.of(override));

    List<MediaOverride> overrides = underTest.getOverrides("default", "ns1:page1");
    assertEquals(1, overrides.size());
  }

  @Test
  void test_getOverridesForImage() {
    MediaOverride override =
        new MediaOverride("site", "ns1", "page1", "ns1", "img1.jpg", "ns1", "img2.jpg");
    when(repo.findAllBySiteAndNewTargetFileNSAndNewTargetFileName("default", "ns1", "img1.jpg"))
        .thenReturn(List.of(override));

    List<MediaOverride> overrides = underTest.getOverridesForImage("default", "ns1:img1.jpg");
    assertEquals(1, overrides.size());
  }

  @Test
  void test_createOverride() {
    List<ImageRef> links = List.of(new ImageRef("default", "ns1", "page1", "ns1", "img1.jpg"));
    when(imageRefRepository.findAllBySiteAndImageNSAndImageRef("default", "ns1", "img1.jpg"))
        .thenReturn(links);
    List<MediaOverride> existingOverrides =
        List.of(new MediaOverride("default", "ns1", "page5", "ns", "img.jpg", "ns1", "img1.jpg"));
    when(repo.findAllBySiteAndNewTargetFileNSAndNewTargetFileName("default", "ns1", "img1.jpg"))
        .thenReturn(existingOverrides);
    when(imageRefRepository.findAllBySiteAndImageNSAndImageRef("default", "ns1", "img1.jpg"))
        .thenReturn(links);
    underTest.createOverride("default", "ns1", "img1.jpg", "ns2", "img2.jpg");

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
  void test_moveOverrides() {
    List<MediaOverride> existingOverrides =
        List.of(new MediaOverride("default", "ns1", "page1", "ns", "img.jpg", "ns1", "img1.jpg"));
    when(repo.findAllBySiteAndSourcePageNSAndSourcePageNameOrderById("default", "ns1", "page1"))
        .thenReturn(existingOverrides);
    underTest.moveOverrides("default", "ns1:page1", "ns2:page2");

    verify(repo).deleteBySiteAndSourcePageNSAndSourcePageName("default", "ns1", "page1");
    List<MediaOverride> newOverrides =
        List.of(new MediaOverride("default", "ns2", "page2", "ns", "img.jpg", "ns1", "img1.jpg"));
    verify(repo).saveAll(newOverrides);
  }

  @Test
  void test_deleteOverrides() {
    underTest.deleteOverrides("default", "ns1:page1");

    verify(repo).deleteBySiteAndSourcePageNSAndSourcePageName("default", "ns1", "page1");
  }
}
