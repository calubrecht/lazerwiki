package us.calubrecht.lazerwiki.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.model.*;
import us.calubrecht.lazerwiki.repository.*;
import us.calubrecht.lazerwiki.responses.MoveStatus;
import us.calubrecht.lazerwiki.responses.PageLockResponse;
import us.calubrecht.lazerwiki.service.exception.PageRevisionException;
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
    LinkOverrideService linkOverrideService;

    @MockBean
    ImageRefService imageRefService;
    @MockBean
    PageCacheRepository pageCacheRepository;

    @MockBean
    RegenCacheService regenCacheService;

    @MockBean
    PageLockService pageLockService;


    @MockBean
    EntityManagerProxy em;

    @MockBean
    TagRepository tagRepository;

    @MockBean
    UserService userService;

    @MockBean
    ActivityLogService activityLogService;


    @Test
    public void testSavePage() throws PageWriteException {
        when(idRepository.getNewId()).thenReturn(55L);
        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");
        when(namespaceService.canReadNamespace(eq("site1"), any(), eq("someUser"))).thenReturn(true);
        when(namespaceService.canWriteNamespace(eq("site1"), any(), eq("someUser"))).thenReturn(true);
        User user =new User("someUser", "hash");
        when(userService.getUser("someUser")).thenReturn(user);

        pageUpdateService.savePage("host1", "newPage", 0L, "Some text", Collections.emptyList(),  Collections.emptyList(),Collections.emptyList(),"Title","someUser", false);
        ArgumentCaptor<Page> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(pageRepository).save(pageCaptor.capture());
        // new Page, should regen cache
        verify(regenCacheService).regenCachesForBacklinks("site1", "newPage");
        verify(pageLockService).releaseAnyPageLock("host1", "newPage");
        verify(activityLogService).log(ActivityType.ACTIVITY_PROTO_CREATE_PAGE, "site1",user, "newPage");
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
        User user =new User("someUser", "hash");
        when(userService.getUser("someUser")).thenReturn(user);

        pageUpdateService.savePage("host1", "ns:realPage", 2L, "Some text", Collections.emptyList(),Collections.emptyList(),  Collections.emptyList(),"Title","someUser", false);
        ArgumentCaptor<Page> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(pageRepository, times(2)).save(pageCaptor.capture());
        verify(regenCacheService, never()).regenCachesForBacklinks(anyString(), anyString());
        verify(activityLogService).log(ActivityType.ACTIVITY_PROTO_MODIFY_PAGE,  "site1",user, "ns:realPage");

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
        User user =new User("someUser", "hash");
        when(userService.getUser("someUser")).thenReturn(user);

        pageUpdateService.savePage("host1", "deletedPage", 2L, "Some text", Collections.emptyList(),Collections.emptyList(),  Collections.emptyList(),"Title","someUser", false);
        ArgumentCaptor<Page> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(pageRepository, times(2)).save(pageCaptor.capture());
        verify(regenCacheService).regenCachesForBacklinks("site1", "deletedPage");
        verify(activityLogService).log(ActivityType.ACTIVITY_PROTO_CREATE_PAGE,  "site1",user, "deletedPage");
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
        when(userService.getUser("Joe")).thenReturn(new User("someUser", "hash"));

        assertThrows(PageWriteException.class, () ->
                pageUpdateService.savePage("host1", "newPage", 0L,"Some text", null,  Collections.emptyList(),Collections.emptyList(),"Title","Joe", false));
    }

    @Test
    public void testSavePageWithLinks() throws PageWriteException {
        when(idRepository.getNewId()).thenReturn(55L);
        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");
        when(namespaceService.canReadNamespace(eq("site1"), any(), eq("someUser"))).thenReturn(true);
        when(namespaceService.canWriteNamespace(eq("site1"), any(), eq("someUser"))).thenReturn(true);
        when(userService.getUser("someUser")).thenReturn(new User("someUser", "hash"));

        pageUpdateService.savePage("host1", "newPage", 0L, "Some text", Collections.emptyList(),  List.of("page1", "page2"),Collections.emptyList(),"Title","someUser", false);

        verify(linkService).setLinksFromPage("site1", "", "newPage",  List.of("page1", "page2"));
    }

    @Test
    public void testSavePageWithImages() throws PageWriteException {
        when(idRepository.getNewId()).thenReturn(55L);
        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");
        when(namespaceService.canReadNamespace(eq("site1"), any(), eq("someUser"))).thenReturn(true);
        when(namespaceService.canWriteNamespace(eq("site1"), any(), eq("someUser"))).thenReturn(true);
        when(userService.getUser("someUser")).thenReturn(new User("someUser", "hash"));

        pageUpdateService.savePage("host1", "newPage", 0L, "Some text", Collections.emptyList(),  List.of("page1", "page2"), List.of("image1.jpg", "image2.jpg"),"Title","someUser", false);

        verify(imageRefService).setImageRefsFromPage("site1", "", "newPage",  List.of("image1.jpg", "image2.jpg"));
    }

    @Test
    public void testSavePageRevisionCheck() throws PageWriteException {
        when(idRepository.getNewId()).thenReturn(55L);
        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");
        when(namespaceService.canReadNamespace(eq("site1"), any(), eq("someUser"))).thenReturn(true);
        when(namespaceService.canWriteNamespace(eq("site1"), any(), eq("someUser"))).thenReturn(true);
        Page p = new Page();
        p.setId(10L);
        p.setRevision(2L);
        when(pageRepository.getBySiteAndNamespaceAndPagename("site1", "ns", "realPage")).
                thenReturn(p);
        when(userService.getUser("someUser")).thenReturn(new User("someUser", "hash"));


        assertThrows(PageRevisionException.class, () ->
                pageUpdateService.savePage("host1", "ns:realPage", 1L, "Some text", Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), "Title", "someUser", false));
        verify(pageRepository, Mockito.never()).save(Mockito.any());

        pageUpdateService.savePage("host1", "ns:realPage", 1L, "Some text", Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), "Title", "someUser", true);
        // pageRepository.save called twice, once for old revision, once for new
        verify(pageRepository, Mockito.times(2)).save(Mockito.any());
    }

    @Test
    public void testDeletePage() throws PageWriteException {
        when(siteService.getSiteForHostname(eq("localhost"))).thenReturn("default");
        when(namespaceService.canDeleteInNamespace(eq("default"), eq(""), eq("bob"))).thenReturn(true);
        User user = new User("bob", "hash");
        when(userService.getUser("bob")).thenReturn(user);
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
        verify(activityLogService).log(ActivityType.ACTIVITY_PROTO_DELETE_PAGE,  "default",user, "testPage");

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
        when(userService.getUser("Bob")).thenReturn(new User("Bob", "hash"));

        assertFalse(pageUpdateService.createDefaultSiteHomepage("existingSite", "New Site", "Bob"));
        assertTrue(pageUpdateService.createDefaultSiteHomepage("newSite", "New Site", "Bob"));

        ArgumentCaptor<Page> captor = ArgumentCaptor.forClass(Page.class);
        verify(pageRepository).save(captor.capture());

        assertEquals("newSite", captor.getValue().getSite());
        assertEquals("", captor.getValue().getPagename());
        assertEquals("======New Site======", captor.getValue().getText().split("\n")[0]);
    }

    @Test
    void testMovePage() throws PageWriteException {
        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");
        when(namespaceService.canReadNamespace(eq("site1"), any(), eq("someUser"))).thenReturn(true);
        when(namespaceService.canWriteNamespace(eq("site1"), any(), eq("someUser"))).thenReturn(true);
        when(namespaceService.canDeleteInNamespace(eq("site1"), any(), any())).thenReturn(true);

        PageLockResponse lockSuccess = new PageLockResponse("", "", null, "", null, true, "");
        when(pageLockService.getPageLock(any(), any(), any(), anyBoolean())).thenReturn(lockSuccess);

        Page p = new Page();
        p.setPagename("page1");
        p.setText("This is raw page text");
        p.setTitle("Title");
        p.setId(10L);
        p.setRevision(2L);
        p.setSite("site1");
        p.setTags(Collections.emptyList());
        when(pageRepository.getBySiteAndNamespaceAndPagename("site1", "ns1", "page1")).thenReturn(p);
        when(pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted("site1", "ns1", "page1", false)).thenReturn(p);
        User user = new User("someUser", "hash");
        when(userService.getUser("someUser")).thenReturn(user);

        pageUpdateService.movePage("host1", "someUser", "ns1", "page1", "ns2", "page2");

        verify(linkOverrideService).createOverride("host1", "ns1:page1", "ns2:page2");
        verify(linkOverrideService).moveOverrides("host1", "ns1:page1", "ns2:page2");

        ArgumentCaptor<Page> captor = ArgumentCaptor.forClass(Page.class);
        verify(pageRepository, times(3)).save(captor.capture());
        verify(activityLogService).log(ActivityType.ACTIVITY_PROTO_DELETE_PAGE, "site1", user, "ns1:page1");
        verify(activityLogService).log(ActivityType.ACTIVITY_PROTO_CREATE_PAGE, "site1",user, "ns2:page2");
        verify(activityLogService).log(ActivityType.ACTIVITY_PROTO_MOVE_PAGE, "site1",user, "ns1:page1->ns2:page2");

        List<Page> pages = captor.getAllValues();
        assertEquals("page2", pages.get(0).getPagename());
        assertEquals("page1", pages.get(1).getPagename());
        assertFalse(pages.get(1).isDeleted());
        assertEquals("page1", pages.get(2).getPagename());
        assertTrue(pages.get(2).isDeleted());


        // Can mvoe to page if already deleted
        Page deleted = new Page();
        deleted.setDeleted(true);
        deleted.setRevision(10L);
        deleted.setId(5L);
        when(pageRepository.getBySiteAndNamespaceAndPagename("site1", "ns1", "deleted")).thenReturn(deleted);
        Page oldPage = new Page();
        oldPage.setTags(Collections.emptyList());
        oldPage.setRevision(1L);
        oldPage.setId(11L);
        when(pageRepository.getBySiteAndNamespaceAndPagename("site1", "ns1", "page2")).thenReturn(oldPage);
        MoveStatus status = pageUpdateService.movePage("host1", "someUser", "ns1", "page2", "ns1", "deleted");
        assertEquals(true, status.success());
    }

    @Test
    void testMovePageFails() throws PageWriteException {
        when(siteService.getSiteForHostname(eq("host1"))).thenReturn("site1");
        when(namespaceService.canWriteNamespace(eq("site1"), any(), eq("user"))).thenReturn(true);
        when(namespaceService.canWriteNamespace(eq("site1"), eq("ns1"), eq("user2"))).thenReturn(true);
        MoveStatus status = pageUpdateService.movePage("host1", "loser", "ns1", "page1", "ns2", "page2");

        assertEquals(false, status.success());
        assertEquals("You don't have permission to write in ns1", status.message());

        status = pageUpdateService.movePage("host1", "user2", "ns1", "page1", "ns2", "page2");
        assertEquals(false, status.success());
        assertEquals("You don't have permission to write in ns2", status.message());


        PageLockResponse lockSuccess = new PageLockResponse("", "", null, "", null, true, "");
        PageLockResponse lockFail = new PageLockResponse("", "", null, "", null, false, "");
        when(pageLockService.getPageLock(any(), eq("noLock:page1"), any(), anyBoolean())).thenReturn(lockFail);
        when(pageLockService.getPageLock(any(), eq("lockable:page1"), any(), anyBoolean())).thenReturn(lockSuccess);

        status = pageUpdateService.movePage("host1", "user", "noLock", "page1", "lockable", "page1");
        assertEquals(false, status.success());
        assertEquals("Could not acquire page locks to move page", status.message());

        status = pageUpdateService.movePage("host1", "user", "lockable", "page1", "noLock", "page1");
        assertEquals(false, status.success());
        assertEquals("Could not acquire page locks to move page", status.message());

        when(pageRepository.getBySiteAndNamespaceAndPagename("site1", "lockable", "page3")).thenReturn(new Page());
        status = pageUpdateService.movePage("host1", "user", "lockable", "page2", "lockable", "page3");
        assertEquals(false, status.success());
        assertEquals("page3 already exists, move cannot overwrite it", status.message());
    }
}
