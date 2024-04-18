package us.calubrecht.lazerwiki.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.model.Page;
import us.calubrecht.lazerwiki.repository.*;
import us.calubrecht.lazerwiki.service.exception.PageWriteException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@SpringBootTest(classes = {PageUpdateService.class})
@ActiveProfiles("test")
public class PageUpdateServiceTest {

    @Autowired
    PageUpdateService pageUpdateService;

    @MockBean
    PageRepository pageRepository;

    @MockBean
    IdRepository idRepository;

    @MockBean
    SiteService siteService;

    @MockBean
    NamespaceService namespaceService;

    @MockBean
    LinkService linkService;

    @MockBean
    ImageRefService imageRefService;
    @MockBean
    PageCacheRepository pageCacheRepository;

    @MockBean
    RegenCacheService regenCacheService;


    @MockBean
    EntityManagerProxy em;

    @MockBean
    TagRepository tagRepository;


    @Test
    public void testSavePage() throws PageWriteException {
        when(idRepository.getNewId()).thenReturn(55L);
        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");
        when(namespaceService.canReadNamespace(eq("site1"), any(), eq("someUser"))).thenReturn(true);
        when(namespaceService.canWriteNamespace(eq("site1"), any(), eq("someUser"))).thenReturn(true);

        pageUpdateService.savePage("host1", "newPage", "Some text", Collections.emptyList(),  Collections.emptyList(),Collections.emptyList(),"Title","someUser");
        ArgumentCaptor<Page> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(pageRepository).save(pageCaptor.capture());
        // new Page, should regen cache
        verify(regenCacheService).regenCachesForBacklinks("site1", "newPage");
        Page p = pageCaptor.getValue();
        assertEquals("Some text", p.getText());
        assertEquals(55L, p.getId());
        assertEquals("site1", p.getSite());
        assertEquals("someUser", p.getModifiedBy());
    }

    @Test
    public void testSavePage_Existing() throws PageWriteException {
        when(idRepository.getNewId()).thenReturn(55L);
        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");
        when(namespaceService.canReadNamespace(eq("site1"), any(), eq("someUser"))).thenReturn(true);
        when(namespaceService.canWriteNamespace(eq("site1"), any(), eq("someUser"))).thenReturn(true);
        Page p = new Page();
        p.setText("This is raw page text");
        p.setTitle("Title");
        p.setId(10L);
        p.setRevision(2L);
        p.setSite("site1");
        p.setTags(Collections.emptyList());
        when(pageRepository.getBySiteAndNamespaceAndPagename("site1","ns", "realPage")).
                thenReturn(p);

        pageUpdateService.savePage("host1", "ns:realPage", "Some text", Collections.emptyList(),Collections.emptyList(),  Collections.emptyList(),"Title","someUser");
        ArgumentCaptor<Page> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(pageRepository, times(2)).save(pageCaptor.capture());
        verify(regenCacheService, never()).regenCachesForBacklinks(anyString(), anyString());

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
    public void testSavePageDeleted() throws PageWriteException {
        when(idRepository.getNewId()).thenReturn(55L);
        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");
        when(namespaceService.canReadNamespace(eq("site1"), any(), eq("someUser"))).thenReturn(true);
        when(namespaceService.canWriteNamespace(eq("site1"), any(), eq("someUser"))).thenReturn(true);
        Page p = new Page();
        p.setText("This is raw page text");
        p.setTitle("Title");
        p.setId(10L);
        p.setRevision(2L);
        p.setSite("site1");
        p.setTags(Collections.emptyList());
        p.setDeleted(true);
        when(pageRepository.getBySiteAndNamespaceAndPagename("site1","", "deletedPage")).
                thenReturn(p);

        pageUpdateService.savePage("host1", "deletedPage", "Some text", Collections.emptyList(),Collections.emptyList(),  Collections.emptyList(),"Title","someUser");
        ArgumentCaptor<Page> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(pageRepository, times(2)).save(pageCaptor.capture());
        verify(regenCacheService).regenCachesForBacklinks("site1", "deletedPage");
        Page pSaved = pageCaptor.getAllValues().get(1);  // Second saved page is restore page.
        assertEquals("Some text", pSaved.getText());
        assertEquals(10L, pSaved.getId());  // Id from deleted page is reused
        assertEquals("site1", pSaved.getSite());
        assertEquals("someUser", pSaved.getModifiedBy());
    }

    @Test
    public void testSavePage_unauthorized()  {
        when(idRepository.getNewId()).thenReturn(55L);
        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");

        assertThrows(PageWriteException.class, () ->
                pageUpdateService.savePage("host1", "newPage", "Some text", null,  Collections.emptyList(),Collections.emptyList(),"Title","Joe"));
    }

    @Test
    public void testSavePageWithLinks() throws PageWriteException {
        when(idRepository.getNewId()).thenReturn(55L);
        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");
        when(namespaceService.canReadNamespace(eq("site1"), any(), eq("someUser"))).thenReturn(true);
        when(namespaceService.canWriteNamespace(eq("site1"), any(), eq("someUser"))).thenReturn(true);

        pageUpdateService.savePage("host1", "newPage", "Some text", Collections.emptyList(),  List.of("page1", "page2"),Collections.emptyList(),"Title","someUser");

        verify(linkService).setLinksFromPage("site1", "", "newPage",  List.of("page1", "page2"));
    }

    @Test
    public void testSavePageWithImages() throws PageWriteException {
        when(idRepository.getNewId()).thenReturn(55L);
        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");
        when(namespaceService.canReadNamespace(eq("site1"), any(), eq("someUser"))).thenReturn(true);
        when(namespaceService.canWriteNamespace(eq("site1"), any(), eq("someUser"))).thenReturn(true);

        pageUpdateService.savePage("host1", "newPage", "Some text", Collections.emptyList(),  List.of("page1", "page2"), List.of("image1.jpg", "image2.jpg"),"Title","someUser");

        verify(imageRefService).setImageRefsFromPage("site1", "", "newPage",  List.of("image1.jpg", "image2.jpg"));
    }

    @Test
    public void testDeletePage() throws PageWriteException {
        when(siteService.getSiteForHostname(eq("localhost"))).thenReturn("default");
        when(namespaceService.canDeleteInNamespace(eq("default"), eq(""), eq("bob"))).thenReturn(true);
        Page p = new Page();
        p.setId(1000L);
        p.setPagename("testPage");
        p.setNamespace("");
        p.setDeleted(false);
        p.setRevision(10L);
        when(pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted(eq("default"), eq(""), eq("testPage"), eq(false))).thenReturn(p);

        assertThrows(PageWriteException.class, ()-> pageUpdateService.deletePage("localhost", "testPage", "frank"));

        assertThrows(PageWriteException.class, ()-> pageUpdateService.deletePage("localhost", "", "bob"));

        verify(linkService, never()).deleteLinks(anyString(), anyString());

        pageUpdateService.deletePage("localhost", "unknownPage", "bob");
        verify(linkService, never()).deleteLinks(anyString(), anyString());

        pageUpdateService.deletePage("localhost", "testPage", "bob");
        verify(linkService).deleteLinks(eq("default"), eq("testPage"));
        verify(regenCacheService).regenCachesForBacklinks("default", "testPage");
        ArgumentCaptor<Page> captor = ArgumentCaptor.forClass(Page.class);
        verify(pageRepository, times(2)).save(captor.capture());

        assertEquals(2, captor.getAllValues().size());
        Page invalidatedPage = captor.getAllValues().get(0);
        assertEquals(1000L, invalidatedPage.getId());
        assertEquals(10L, invalidatedPage.getRevision());
        Page newPage = captor.getAllValues().get(1);
        assertEquals("", newPage.getText());
        assertEquals(1000L, newPage.getId());
        assertEquals(11L, newPage.getRevision());
        assertTrue(newPage.isDeleted());
    }


    @Test
    public void testCreateDefaultSiteHomepage() throws PageWriteException, IOException {
        when(pageRepository.getBySiteAndNamespaceAndPagename("existingSite", "", "")).thenReturn(new Page());
        when(namespaceService.canWriteNamespace(eq("newSite"), any(), eq("Bob"))).thenReturn(true);
        when(siteService.getSiteForHostname("site.com")).thenReturn("newSite");
        when(siteService.getHostForSitename("newSite")).thenReturn("site.com");

        assertFalse(pageUpdateService.createDefaultSiteHomepage("existingSite", "New Site", "Bob"));
        assertTrue(pageUpdateService.createDefaultSiteHomepage("newSite", "New Site", "Bob"));

        ArgumentCaptor<Page> captor = ArgumentCaptor.forClass(Page.class);
        verify(pageRepository).save(captor.capture());

        assertEquals("newSite", captor.getValue().getSite());
        assertEquals("", captor.getValue().getPagename());
        assertEquals("======New Site======", captor.getValue().getText().split("\n")[0]);
    }
}
