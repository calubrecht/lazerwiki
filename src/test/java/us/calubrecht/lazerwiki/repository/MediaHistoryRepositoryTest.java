package us.calubrecht.lazerwiki.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Limit;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.LazerWikiApplication;
import us.calubrecht.lazerwiki.model.MediaHistoryRecord;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {LazerWikiApplication.class})
@ActiveProfiles("test")
class MediaHistoryRepositoryTest {

    @Autowired
    MediaHistoryRepository repo;

    @Test
    void getAllNamespaces() {
        List<String> nses = repo.getAllNamespaces("site1");

        assertEquals(List.of("ns1", "ns4"), nses);
    }

    @Test
    void findAllBySiteAndNamespaceInOrderByTsDesc() {
        List<MediaHistoryRecord> records = repo.findAllBySiteAndNamespaceInOrderByTsDesc(Limit.of(10), "site1", List.of("ns1"));

        assertEquals(2, records.size());
        assertEquals("img1.jpg", records.get(0).getFileName());
        assertEquals("img1.jpg", records.get(1).getFileName());

        records = repo.findAllBySiteAndNamespaceInOrderByTsDesc(Limit.of(10), "site1", List.of("ns1", "ns4"));

        assertEquals(3, records.size());
        assertEquals("img3.jpg", records.get(0).getFileName());
        assertEquals("img1.jpg", records.get(1).getFileName());
        assertEquals("img1.jpg", records.get(2).getFileName());
    }
}