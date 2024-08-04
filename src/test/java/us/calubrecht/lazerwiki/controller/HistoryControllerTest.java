package us.calubrecht.lazerwiki.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import us.calubrecht.lazerwiki.model.MediaHistoryRecord;
import us.calubrecht.lazerwiki.model.PageDesc;
import us.calubrecht.lazerwiki.model.RecentChangesResponse;
import us.calubrecht.lazerwiki.service.MediaService;
import us.calubrecht.lazerwiki.service.PageService;
import us.calubrecht.lazerwiki.service.PageServiceTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(controllers = {HistoryController.class, VersionController.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class HistoryControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    HistoryController controller;

    @MockBean
    MediaService mediaService;

    @MockBean
    PageService pageService;

    @Test
    void recentChanges() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken("Bob", "password1");

        List<MediaHistoryRecord> medias = new ArrayList<>();
        medias.add(new MediaHistoryRecord("img1.jpg", "site1", "ns", "Bob", "Uploaed"));
        medias.get(0).setTs(LocalDateTime.of(2022,01,01,10,10,0));
        medias.add(new MediaHistoryRecord("img2.jpg", "site1", "ns", "Bob", "Uploaed"));
        medias.get(1).setTs(LocalDateTime.of(2021,06,01,10,10,0));
        List<PageDesc> pages = new ArrayList<>();
        pages.add(new PageServiceTest.PageDescImpl("ns", "page1", 2L));
        ((PageServiceTest.PageDescImpl)pages.get(0)).setModified(LocalDateTime.of(2021,11,01,10,10,0));
        pages.add(new PageServiceTest.PageDescImpl("ns", "page2", 1L));
        ((PageServiceTest.PageDescImpl)pages.get(1)).setModified(LocalDateTime.of(2021,10,01,10,10,0));
        RecentChangesResponse pagesRes = new RecentChangesResponse(pages.stream().map(RecentChangesResponse::recFor).toList(), null, null);

        when(mediaService.getRecentChanges("localhost", "Bob")).thenReturn(medias);
        when(pageService.recentChanges("localhost", "Bob")).thenReturn(pagesRes);
        this.mockMvc.perform(get("/api/history/recentChanges").
                        principal(auth)).
                andExpect(status().isOk());

        verify(mediaService).getRecentChanges(eq("localhost"), eq("Bob"));
        verify(pageService).recentChanges(eq("localhost"), eq("Bob"));

        when(mediaService.getRecentChanges("localhost", "Guest")).thenReturn(Collections.emptyList());
        when(pageService.recentChanges("localhost", "Guest")).thenReturn(new RecentChangesResponse(Collections.emptyList(), null, null));
        this.mockMvc.perform(get("/api/history/recentChanges")).
                andExpect(status().isOk());
        verify(mediaService).getRecentChanges(eq("localhost"), eq("Guest"));
        verify(pageService).recentChanges(eq("localhost"), eq("Guest"));

    }

    @Test
    void mergePageAndMedia() {
        // Simple Merge
        List<MediaHistoryRecord> medias = new ArrayList<>();
        medias.add(new MediaHistoryRecord("img1.jpg", "site1", "ns", "Bob", "Uploaed"));
        medias.get(0).setTs(LocalDateTime.of(2022,01,01,10,10,0));
        medias.add(new MediaHistoryRecord("img2.jpg", "site1", "ns", "Bob", "Uploaed"));
        medias.get(1).setTs(LocalDateTime.of(2021,06,01,10,10,0));
        List<PageDesc> pages = new ArrayList<>();
        pages.add(new PageServiceTest.PageDescImpl("ns", "page1", 2L));
        ((PageServiceTest.PageDescImpl)pages.get(0)).setModified(LocalDateTime.of(2021,11,01,10,10,0));
        pages.add(new PageServiceTest.PageDescImpl("ns", "page2", 1L));
        ((PageServiceTest.PageDescImpl)pages.get(1)).setModified(LocalDateTime.of(2021,10,01,10,10,0));
        RecentChangesResponse pagesRes = new RecentChangesResponse(pages.stream().map(RecentChangesResponse::recFor).toList(), null, null);

        List<Object> merged = controller.mergePageAndMedia(pagesRes.changes(), medias);
        assertEquals(4, merged.size());
        assertInstanceOf(MediaHistoryRecord.class, merged.get(0));
        assertInstanceOf(RecentChangesResponse.RecentChangeRec.class, merged.get(1));
        assertInstanceOf(RecentChangesResponse.RecentChangeRec.class, merged.get(2));
        assertInstanceOf(MediaHistoryRecord.class, merged.get(3));

        // Both empty case
        merged = controller.mergePageAndMedia(new ArrayList<>(), new ArrayList<>());
        assertEquals(0, merged.size());

        // Page empty
        merged = controller.mergePageAndMedia(new ArrayList<>(), medias);
        assertEquals(medias, merged);

        // media empty
        merged = controller.mergePageAndMedia(pagesRes.changes(), new ArrayList<>());
        assertEquals(pagesRes.changes(), merged);

        // Media runs out first
        pages.add(new PageServiceTest.PageDescImpl("ns", "page2", 1L));
        ((PageServiceTest.PageDescImpl)pages.get(2)).setModified(LocalDateTime.of(2020,10,01,10,10,0));
        pages.add(new PageServiceTest.PageDescImpl("ns", "page2", 1L));
        ((PageServiceTest.PageDescImpl)pages.get(3)).setModified(LocalDateTime.of(2019,10,01,10,10,0));
        pages.add(new PageServiceTest.PageDescImpl("ns", "page2", 1L));
        ((PageServiceTest.PageDescImpl)pages.get(4)).setModified(LocalDateTime.of(2018,10,01,10,10,0));
        pages.add(new PageServiceTest.PageDescImpl("ns", "page2", 1L));
        ((PageServiceTest.PageDescImpl)pages.get(5)).setModified(LocalDateTime.of(2012,10,01,10,10,0));
        pagesRes = new RecentChangesResponse(pages.stream().map(RecentChangesResponse::recFor).toList(), null, null);

        merged = controller.mergePageAndMedia(pagesRes.changes(), medias);
        assertEquals(8, merged.size());
        assertInstanceOf(RecentChangesResponse.RecentChangeRec.class, merged.get(4));
        assertInstanceOf(RecentChangesResponse.RecentChangeRec.class, merged.get(5));
        assertInstanceOf(RecentChangesResponse.RecentChangeRec.class, merged.get(6));
        assertInstanceOf(RecentChangesResponse.RecentChangeRec.class, merged.get(7));

        // More than 10 total.
        medias.add(new MediaHistoryRecord("img2.jpg", "site1", "ns", "Bob", "Uploaed"));
        medias.get(2).setTs(LocalDateTime.of(2020,06,01,10,10,0));
        medias.add(new MediaHistoryRecord("img2.jpg", "site1", "ns", "Bob", "Uploaed"));
        medias.get(3).setTs(LocalDateTime.of(2019,06,01,10,10,0));
        medias.add(new MediaHistoryRecord("img2.jpg", "site1", "ns", "Bob", "Uploaed"));
        medias.get(4).setTs(LocalDateTime.of(2018,06,01,10,10,0));
        medias.add(new MediaHistoryRecord("img2.jpg", "site1", "ns", "Bob", "Uploaed"));
        medias.get(5).setTs(LocalDateTime.of(2017,06,01,10,10,0));
        pages.add(new PageServiceTest.PageDescImpl("ns", "page2", 1L));
        ((PageServiceTest.PageDescImpl)pages.get(2)).setModified(LocalDateTime.of(2020,10,01,10,10,0));
        pages.add(new PageServiceTest.PageDescImpl("ns", "page2", 1L));
        ((PageServiceTest.PageDescImpl)pages.get(3)).setModified(LocalDateTime.of(2019,10,01,10,10,0));
        pages.add(new PageServiceTest.PageDescImpl("ns", "page2", 1L));
        ((PageServiceTest.PageDescImpl)pages.get(4)).setModified(LocalDateTime.of(2018,10,01,10,10,0));
        pages.add(new PageServiceTest.PageDescImpl("ns", "page2", 1L));
        ((PageServiceTest.PageDescImpl)pages.get(5)).setModified(LocalDateTime.of(2012,10,01,10,10,0));
        pagesRes = new RecentChangesResponse(pages.stream().map(RecentChangesResponse::recFor).toList(), null, null);

        merged = controller.mergePageAndMedia(pagesRes.changes(), medias);
        assertEquals(10, merged.size());
    }
}