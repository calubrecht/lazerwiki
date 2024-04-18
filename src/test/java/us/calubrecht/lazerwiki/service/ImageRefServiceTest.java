package us.calubrecht.lazerwiki.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.model.ImageRef;
import us.calubrecht.lazerwiki.repository.ImageRefRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {ImageRefService.class})
@ActiveProfiles("test")
class ImageRefServiceTest {
    @Autowired
    ImageRefService underTest;

    @MockBean
    ImageRefRepository imageRefRepository;

    @Test
    void setImageRefsFromPage() {
        underTest.setImageRefsFromPage("default", "ns", "page1", List.of("image1.jpg", "ns2:image2.jpg"));

        verify(imageRefRepository).deleteBySiteAndSourcePageNSAndSourcePageName("default", "ns", "page1");
        verify(imageRefRepository).saveAll(List.of(new ImageRef("default", "ns", "page1", "", "image1.jpg"), new ImageRef("default", "ns", "page1", "ns2", "image2.jpg")));
    }

    @Test
    void getImagesOnPage() {
        when(imageRefRepository.findAllBySiteAndSourcePageNSAndSourcePageName("default", "ns2", "page1")).thenReturn(
                List.of(new ImageRef("default", "ns", "page1", "", "img1.jpg"), new ImageRef("default", "ns", "page1", "ns", "img2.jpg"))
        );

        List<String> imageRefs = underTest.getImagesOnPage("default", "ns2:page1");

        assertEquals(List.of("img1.jpg", "ns:img2.jpg"), imageRefs);
    }

    @Test
    void getRefsForImage() {
        when(imageRefRepository.findAllBySiteAndImageNSAndImageRef("default", "ns2", "image1.jpg")).thenReturn(
                List.of(new ImageRef("default", "", "page1", "ns2", "image1.jpg"), new ImageRef("default", "ns", "page2", "ns2", "image1.jpg"))
        );
        List<String> imageRefs = underTest.getRefsForImage("default", "ns2:image1.jpg");

        assertEquals(List.of("page1", "ns:page2"), imageRefs);
    }

    @Test
    void deleteImageRefs() {
        underTest.deleteImageRefs("default", "ns2:page2");

        verify(imageRefRepository).deleteBySiteAndSourcePageNSAndSourcePageName("default", "ns2", "page2");
    }
}