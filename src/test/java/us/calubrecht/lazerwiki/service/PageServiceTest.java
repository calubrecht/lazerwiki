package us.calubrecht.lazerwiki.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.model.Page;
import us.calubrecht.lazerwiki.model.PageData;
import us.calubrecht.lazerwiki.model.PageDescriptor;
import us.calubrecht.lazerwiki.repository.EntityManagerProxy;
import us.calubrecht.lazerwiki.repository.IdRepository;
import us.calubrecht.lazerwiki.repository.PageRepository;
import us.calubrecht.lazerwiki.service.exception.PageWriteException;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {PageService.class})
@ActiveProfiles("test")
public class PageServiceTest {

    @Autowired
    PageService pageService;

    @MockBean
    PageRepository pageRepository;

    @MockBean
    IdRepository idRepository;

    @MockBean
    SiteService siteService;

    @MockBean
    EntityManagerProxy em;

    @Test
    public void testExists() {
        assertFalse(pageService.exists("site1","nonExistentPage"));

        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");
        when(pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted("site1", "ns", "realPage", false)).
                thenReturn(new Page());
        assertTrue(pageService.exists("host1", "ns:realPage"));
    }

    @Test
    public void getTitle() {
        assertEquals("Non Existent Page", pageService.getTitle("site1", "some:ns:nonExistentPage"));

        Page p = new Page();
        p.setTitle("Titled Page");
        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");
        when(pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted("site1", "some:ns", "realPage", false)).
                thenReturn(p);
        assertEquals("Titled Page", pageService.getTitle("host1", "some:ns:realPage"));

        Page p2 = new Page();
        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");
        when(pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted("site1", "some:ns", "nonTitledRealPage", false)).
                thenReturn(p2);
        assertEquals("Non Titled Real Page", pageService.getTitle("host1", "some:ns:nonTitledRealPage"));
    }

    @Test
    public void testGetPageData() {
        assertEquals(new PageData("This page doesn't exist", "", false), pageService.getPageData("localhost", "nonExistantPage", "Bob"));

        Page p = new Page();
        p.setText("This is raw page text");
        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");
        when(pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted("site1","ns", "realPage", false)).
                thenReturn(p);

        assertEquals(new PageData(null, "This is raw page text", true), pageService.getPageData("host1", "ns:realPage", "Bob"));
    }

    @Test
    public void testDecodeDescriptor() {
        assertEquals(new PageDescriptor("", "noNS"), pageService.decodeDescriptor("noNS"));
        assertEquals(new PageDescriptor("ns", "withNS"), pageService.decodeDescriptor("ns:withNS"));
        assertEquals("ns:withNS", pageService.decodeDescriptor("ns:withNS").toString());
    }

    @Test
    public void testSavePage() throws PageWriteException {
        when(idRepository.getNewId()).thenReturn(55L);
        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");

        pageService.savePage("host1", "newPage", "Some text", "someUser");
        ArgumentCaptor<Page> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(pageRepository).save(pageCaptor.capture());
        Page p = pageCaptor.getValue();
        assertEquals("Some text", p.getText());
        assertEquals(55L, p.getId());
        assertEquals("site1", p.getSite());
    }

    @Test
    public void testSavePage_Existing() throws PageWriteException {
        when(idRepository.getNewId()).thenReturn(55L);
        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");
        Page p = new Page();
        p.setText("This is raw page text");
        p.setId(10L);
        p.setRevision(2L);
        p.setSite("site1");
        when(pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted("site1","ns", "realPage", false)).
                thenReturn(p);

        pageService.savePage("host1", "ns:realPage", "Some text", "someUser");
        ArgumentCaptor<Page> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(pageRepository, times(2)).save(pageCaptor.capture());

        assertEquals(2, pageCaptor.getAllValues().size());
        Page invalidatedPage = pageCaptor.getAllValues().get(0);
        assertEquals("This is raw page text", invalidatedPage.getText());
        assertEquals(10L, invalidatedPage.getId());
        assertEquals(2L, invalidatedPage.getRevision());
        assertEquals("site1", invalidatedPage.getSite());
        Page newPage = pageCaptor.getAllValues().get(1);
        assertEquals("Some text", newPage.getText());
        assertEquals(10L, newPage.getId());
        assertEquals(3L, newPage.getRevision());
        assertEquals("site1", newPage.getSite());
    }

    @Test
    public void testSavePage_unauthorized()  {
        when(idRepository.getNewId()).thenReturn(55L);
        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");

        assertThrows(PageWriteException.class, () ->
                pageService.savePage("host1", "newPage", "Some text", "Joe"));
    }
}
