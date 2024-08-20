package us.calubrecht.lazerwiki.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.model.PageLock;
import us.calubrecht.lazerwiki.repository.PageLockRepository;
import us.calubrecht.lazerwiki.repository.PageRepository;
import us.calubrecht.lazerwiki.responses.PageLockResponse;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {PageLockService.class})
@ActiveProfiles("test")
class PageLockServiceTest {

    @Autowired
    PageLockService underTest;

    @MockBean
    PageLockRepository pageLockRepository;

    @MockBean
    PageRepository pageRepository;

    @MockBean
    SiteService siteService;


    @Test
    void getPageLock_getCleanLock() {
        when(siteService.getSiteForHostname("host")).thenReturn("site1");
        when(pageLockRepository.findBySiteAndNamespaceAndPagename("host", "ns", "page1")).thenReturn(null);
        when(pageRepository.getLastRevisionBySiteAndNamespaceAndPagename("site1", "ns", "page1")).thenReturn(1L);

        PageLockResponse r = underTest.getPageLock("host", "ns:page1", "Bob", false);

        // Successfully acquired a lock for user Bob. Returns latest revision and lock valid time
        assertEquals("page1", r.pagename());
        assertEquals("Bob", r.owner());
        assertEquals(true, r.success());
        assertEquals(1L, r.revision());
        assertTrue(r.lockTime().isAfter(LocalDateTime.now()));

        ArgumentCaptor<PageLock> captor = ArgumentCaptor.forClass(PageLock.class);
        verify(pageLockRepository).save(captor.capture());

        assertEquals("Bob", captor.getValue().getOwner());
        assertTrue(LocalDateTime.now().isBefore(captor.getValue().getLockTime()));
    }

    @Test
    void getPageLock_ignoreExpiredLock() {
        when(siteService.getSiteForHostname("host")).thenReturn("site1");
        LocalDateTime lockTime = LocalDateTime.now().minusSeconds(1);
        PageLock lock = new PageLock("default", "ns", "page1", "Joe", lockTime);
        when(pageLockRepository.findBySiteAndNamespaceAndPagename("site1", "ns", "page1")).thenReturn(lock);
        when(pageRepository.getLastRevisionBySiteAndNamespaceAndPagename("site1", "ns", "page1")).thenReturn(1L);

        PageLockResponse r = underTest.getPageLock("host", "ns:page1", "Bob", false);

        // Successfully acquired a lock for user Bob. Returns latest revision and lock valid time
        assertEquals("page1", r.pagename());
        assertEquals("Bob", r.owner());
        assertTrue(r.success());
        assertEquals(1L, r.revision());
        assertTrue(r.lockTime().isAfter(LocalDateTime.now()));

        ArgumentCaptor<PageLock> captor = ArgumentCaptor.forClass(PageLock.class);
        verify(pageLockRepository).save(captor.capture());

        assertEquals("Bob", captor.getValue().getOwner());
        assertTrue(LocalDateTime.now().isBefore(captor.getValue().getLockTime()));
    }

    @Test
    void getPageLock_failIfLocked() {
        when(siteService.getSiteForHostname("host")).thenReturn("site1");
        LocalDateTime lockTime = LocalDateTime.now().plusSeconds(10);
        PageLock lock = new PageLock("default", "ns", "page1", "Joe", lockTime);
        when(pageLockRepository.findBySiteAndNamespaceAndPagename("site1", "ns", "page1")).thenReturn(lock);
        when(pageRepository.getLastRevisionBySiteAndNamespaceAndPagename("site1", "ns", "page1")).thenReturn(1L);

        PageLockResponse r = underTest.getPageLock("host", "ns:page1", "Bob", false);

        // Successfully aquired a lock for user Bob. Returns latest revision and lock valid time
        assertEquals("page1", r.pagename());
        assertEquals("Joe", r.owner());
        assertFalse(r.success());
        assertEquals(1L, r.revision());
        assertTrue(r.lockTime().isAfter(LocalDateTime.now()));

        verify(pageLockRepository, never()).save(any());
    }

    @Test
    void getPageLock_overrideLock() {
        when(siteService.getSiteForHostname("host")).thenReturn("site1");
        LocalDateTime lockTime = LocalDateTime.now().plusSeconds(10);
        PageLock lock = new PageLock("default", "ns", "page1", "Joe", lockTime);
        when(pageLockRepository.findBySiteAndNamespaceAndPagename("site1", "ns", "page1")).thenReturn(lock);
        when(pageRepository.getLastRevisionBySiteAndNamespaceAndPagename("site1", "ns", "page1")).thenReturn(1L);

        PageLockResponse r = underTest.getPageLock("host", "ns:page1", "Bob", true);

        // Successfully aquired a lock for user Bob. Returns latest revision and lock valid time
        assertEquals("page1", r.pagename());
        assertEquals("Bob", r.owner());
        assertTrue(r.success());
        assertEquals(1L, r.revision());
        assertTrue(r.lockTime().isAfter(LocalDateTime.now()));

        ArgumentCaptor<PageLock> captor = ArgumentCaptor.forClass(PageLock.class);
        verify(pageLockRepository).save(captor.capture());

        assertEquals("Bob", captor.getValue().getOwner());
        assertTrue(LocalDateTime.now().isBefore(captor.getValue().getLockTime()));
    }
}