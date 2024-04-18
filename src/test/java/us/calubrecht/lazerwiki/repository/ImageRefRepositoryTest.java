package us.calubrecht.lazerwiki.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import us.calubrecht.lazerwiki.LazerWikiApplication;
import us.calubrecht.lazerwiki.model.ImageRef;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {LazerWikiApplication.class})
@ActiveProfiles("test")
class ImageRefRepositoryTest {

    @Autowired
    ImageRefRepository imageRefRepository;
    @Test
    void findAllBySiteAndSourcePageNSAndSourcePageName() {
        List<ImageRef> refs = imageRefRepository.findAllBySiteAndSourcePageNSAndSourcePageName("site1", "ns1", "page1");
        assertEquals(2, refs.size());
        assertEquals("image1.jpg", refs.get(0).getImageRef());
    }

    @Test
    void findAllBySiteAndImageNSAndImageRef() {
        List<ImageRef> refs = imageRefRepository.findAllBySiteAndImageNSAndImageRef("site1", "", "image1.jpg");
        assertEquals(2, refs.size());
        assertEquals("page1", refs.get(0).getSourcePageName());
    }

    @Test
    @Transactional
    void deleteBySiteAndSourcePageNSAndSourcePageName() {
        List<ImageRef> refs = imageRefRepository.findAllBySiteAndImageNSAndImageRef("site1", "", "image3.jpg");
        assertEquals(2, refs.size());
        assertEquals("page3", refs.get(0).getSourcePageName());
        imageRefRepository.deleteBySiteAndSourcePageNSAndSourcePageName("site1", "ns1", "page3");
        refs = imageRefRepository.findAllBySiteAndImageNSAndImageRef("site1", "", "image3.jpg");
        assertEquals(1, refs.size());
        assertEquals("page4", refs.get(0).getSourcePageName());
    }

    @Test
    @Transactional
    void saveAll() {
        List<ImageRef> refs = imageRefRepository.findAllBySiteAndSourcePageNSAndSourcePageName("site1", "ns1", "page55");
        assertEquals(0, refs.size());

        imageRefRepository.saveAll(List.of (new ImageRef("site1", "ns1", "page55", "", "image22.jpg")));
        refs = imageRefRepository.findAllBySiteAndSourcePageNSAndSourcePageName("site1", "ns1", "page55");
        assertEquals(1, refs.size());
        assertEquals("image22.jpg", refs.get(0).getImageRef());
        assertNotNull(refs.get(0).getId());
    }
}