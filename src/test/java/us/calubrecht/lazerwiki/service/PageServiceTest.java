package us.calubrecht.lazerwiki.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.model.*;
import us.calubrecht.lazerwiki.repository.EntityManagerProxy;
import us.calubrecht.lazerwiki.repository.IdRepository;
import us.calubrecht.lazerwiki.repository.PageRepository;
import us.calubrecht.lazerwiki.service.exception.PageWriteException;


import java.util.ArrayList;
import java.util.List;

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
        when(pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted("site1", "some:ns", "nonTitledRealPage", false)).
                thenReturn(p2);
        assertEquals("Non Titled Real Page", pageService.getTitle("host1", "some:ns:nonTitledRealPage"));

        // If no title, then title of "" should be home
        when(pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted("site1", "", "", false)).
                thenReturn(p2);
        assertEquals("Home", pageService.getTitle("host1", ""));

        when(siteService.getSiteForHostname(eq("host2"))).thenReturn("site2");
        when(pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted("site2", "", "", false)).
                thenReturn(p);
        assertEquals("Titled Page", pageService.getTitle("host2", ""));
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
        assertEquals("someUser", p.getModifiedBy());
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

    @Test
    public void testListPages() {
        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");
        PageDesc page1 = new PageDescImpl("", "page1", "Page 1", "Bob" );
        PageDesc page2 = new PageDescImpl("", "page2", "Page 1","Francis");
        PageDesc page3 = new PageDescImpl("ns1", "page1", "Page 1","Francis");
        PageDesc page4 = new PageDescImpl("ns1:ns2", "page3", "Page 1","Francis");
        List<PageDesc> allPages = List.of(page1, page2, page3, page4);


        when(pageRepository.getAllValid("site1")).thenReturn(allPages);

        PageNode pageTree = pageService.getAllPages("host1");

        assertEquals("", pageTree.getNamespace());
        assertEquals(3, pageTree.getChildren().size());
        assertEquals("ns1", pageTree.getChildren().get(0).getNamespace());
        assertEquals("page1", ((PageNode.TerminalNode)pageTree.getChildren().get(1)).getPage().getPagename());
        assertEquals("", ((PageNode.TerminalNode)pageTree.getChildren().get(1)).getPage().getNamespace());
        assertEquals("page2", ((PageNode.TerminalNode)pageTree.getChildren().get(2)).getPage().getPagename());
        assertEquals("", ((PageNode.TerminalNode)pageTree.getChildren().get(2)).getPage().getNamespace());

        PageNode ns1 = pageTree.getChildren().get(0);
        assertEquals(2, ns1.getChildren().size());
        assertEquals("ns2", ns1.getChildren().get(0).getNamespace());
        assertEquals("page1", ((PageNode.TerminalNode)ns1.getChildren().get(1)).getPage().getPagename());
        assertEquals("ns1", ((PageNode.TerminalNode)ns1.getChildren().get(1)).getPage().getNamespace());

        PageNode ns2 = ns1.getChildren().get(0);
        assertEquals(1, ns2.getChildren().size());
        assertEquals("page3", ((PageNode.TerminalNode)ns2.getChildren().get(0)).getPage().getPagename());
        assertEquals("ns1:ns2", ((PageNode.TerminalNode)ns2.getChildren().get(0)).getPage().getNamespace());
    }


    @Test
    public void testListPages_wEmptyNamespaces() {
        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");
        PageDesc page1 = new PageDescImpl("", "page1", "Page 1", "Bob" );
        PageDesc page2 = new PageDescImpl("ns1:ns2", "page3", "Page 1","Francis");
        List<PageDesc> allPages = List.of(page1, page2);


        when(pageRepository.getAllValid("site1")).thenReturn(allPages);

        PageNode pageTree = pageService.getAllPages("host1");

        assertEquals("", pageTree.getNamespace());
        assertEquals(2, pageTree.getChildren().size());
        assertEquals("ns1", pageTree.getChildren().get(0).getNamespace());

        PageNode ns1 = pageTree.getChildren().get(0);
        assertEquals(1, ns1.getChildren().size());
        assertEquals("ns2", ns1.getChildren().get(0).getNamespace());

        PageNode ns2 = ns1.getChildren().get(0);
        assertEquals(1, ns2.getChildren().size());
        assertEquals("page3", ((PageNode.TerminalNode)ns2.getChildren().get(0)).getPage().getPagename());
        assertEquals("ns1:ns2", ((PageNode.TerminalNode)ns2.getChildren().get(0)).getPage().getNamespace());
    }


    static class PageDescImpl implements PageDesc {
        String namespace;
        String pageName;
        String title;
        String modifiedBy;

        PageDescImpl(String namespace, String pageName, String title, String modifiedBy) {
            this.namespace = namespace;
            this.pageName = pageName;
            this.title = title;
            this.modifiedBy = modifiedBy;
        }

        @Override
        public String getNamespace() {
            return namespace;
        }

        @Override
        public String getPagename() {
            return pageName;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public String getModifiedBy() {
            return modifiedBy;
        }
    }
}
