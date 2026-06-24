package us.calubrecht.lazerwiki.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import us.calubrecht.lazerwiki.model.PageLock;
import us.calubrecht.lazerwiki.model.User;
import us.calubrecht.lazerwiki.repository.PageLockRepository;
import us.calubrecht.lazerwiki.repository.PageRepository;
import us.calubrecht.lazerwiki.responses.PageLockResponse;

@SpringBootTest(classes = {PageLockService.class})
@ActiveProfiles("test")
class PageLockServiceTest {

  @Autowired PageLockService underTest;

  @MockitoBean PageLockRepository pageLockRepository;

  @MockitoBean PageRepository pageRepository;

  @MockitoBean SiteService siteService;

  @MockitoBean UserService userService;

  @Test
  void test_getPageLock_getCleanLock() {
    when(siteService.getSiteForHostname("host")).thenReturn("site1");
    when(pageLockRepository.findBySiteAndNamespaceAndPagename("host", "ns", "page1"))
        .thenReturn(null);
    when(pageRepository.getLastRevisionBySiteAndNamespaceAndPagename("site1", "ns", "page1"))
        .thenReturn(1L);
    User user = new User("Bob", "hash");
    when(userService.getUser("Bob")).thenReturn(user);

    PageLockResponse r = underTest.getPageLock("host", "ns:page1", "Bob", false);

    // Successfully acquired a lock for user Bob. Returns latest revision and lock valid time
    assertEquals("page1", r.pagename());
    assertEquals("Bob", r.owner());
    assertEquals(true, r.success());
    assertEquals(1L, r.revision());
    assertNotNull(r.pageLockId());
    assertEquals(40, r.pageLockId().length());
    assertTrue(r.lockTime().isAfter(LocalDateTime.now()));

    ArgumentCaptor<PageLock> captor = ArgumentCaptor.forClass(PageLock.class);
    verify(pageLockRepository).save(captor.capture());

    assertEquals(user, captor.getValue().getOwner());
    assertTrue(LocalDateTime.now().isBefore(captor.getValue().getLockTime()));
    assertEquals(r.pageLockId(), captor.getValue().getLockId());
  }

  @Test
  void test_getPageLock_ignoreExpiredLock() {
    when(siteService.getSiteForHostname("host")).thenReturn("site1");
    LocalDateTime lockTime = LocalDateTime.now().minusSeconds(1);
    User user = new User("Joe", "hash");
    when(userService.getUser("Joe")).thenReturn(user);
    User bob = new User("Bob", "hash");
    when(userService.getUser("Bob")).thenReturn(bob);

    PageLock lock = new PageLock("default", "ns", "page1", user, lockTime, "id1");
    when(pageLockRepository.findBySiteAndNamespaceAndPagename("site1", "ns", "page1"))
        .thenReturn(lock);
    when(pageRepository.getLastRevisionBySiteAndNamespaceAndPagename("site1", "ns", "page1"))
        .thenReturn(1L);

    PageLockResponse r = underTest.getPageLock("host", "ns:page1", "Bob", false);

    // Successfully acquired a lock for user Bob. Returns latest revision and lock valid time
    assertEquals("page1", r.pagename());
    assertEquals("Bob", r.owner());
    assertTrue(r.success());
    assertEquals(1L, r.revision());
    assertTrue(r.lockTime().isAfter(LocalDateTime.now()));
    assertNotNull(r.pageLockId());
    assertNotEquals("id1", r.pageLockId());

    ArgumentCaptor<PageLock> captor = ArgumentCaptor.forClass(PageLock.class);
    verify(pageLockRepository).save(captor.capture());

    assertEquals(bob, captor.getValue().getOwner());
    assertTrue(LocalDateTime.now().isBefore(captor.getValue().getLockTime()));
    assertEquals(r.pageLockId(), captor.getValue().getLockId());
  }

  @Test
  void test_getPageLock_failIfLocked() {
    when(siteService.getSiteForHostname("host")).thenReturn("site1");
    LocalDateTime lockTime = LocalDateTime.now().plusSeconds(10);
    User user = new User("Joe", "hash");
    when(userService.getUser("Joe")).thenReturn(user);
    PageLock lock = new PageLock("default", "ns", "page1", user, lockTime, "id1");
    when(pageLockRepository.findBySiteAndNamespaceAndPagename("site1", "ns", "page1"))
        .thenReturn(lock);
    when(pageRepository.getLastRevisionBySiteAndNamespaceAndPagename("site1", "ns", "page1"))
        .thenReturn(1L);

    PageLockResponse r = underTest.getPageLock("host", "ns:page1", "Bob", false);

    // Successfully aquired a lock for user Bob. Returns latest revision and lock valid time
    assertEquals("page1", r.pagename());
    assertEquals("Joe", r.owner());
    assertFalse(r.success());
    assertEquals(1L, r.revision());
    assertTrue(r.lockTime().isAfter(LocalDateTime.now()));
    assertNull(r.pageLockId());

    verify(pageLockRepository, never()).save(any());
  }

  @Test
  void test_getPageLock_overrideLock() {
    when(siteService.getSiteForHostname("host")).thenReturn("site1");
    LocalDateTime lockTime = LocalDateTime.now().plusSeconds(10);
    User user = new User("Joe", "hash");
    when(userService.getUser("JOe")).thenReturn(user);
    User bob = new User("Bob", "hash");
    when(userService.getUser("Bob")).thenReturn(bob);
    PageLock lock = new PageLock("default", "ns", "page1", user, lockTime, "id1");
    when(pageLockRepository.findBySiteAndNamespaceAndPagename("site1", "ns", "page1"))
        .thenReturn(lock);
    when(pageRepository.getLastRevisionBySiteAndNamespaceAndPagename("site1", "ns", "page1"))
        .thenReturn(1L);

    PageLockResponse r = underTest.getPageLock("host", "ns:page1", "Bob", true);

    // Successfully aquired a lock for user Bob. Returns latest revision and lock valid time
    assertEquals("page1", r.pagename());
    assertEquals("Bob", r.owner());
    assertTrue(r.success());
    assertEquals(1L, r.revision());
    assertTrue(r.lockTime().isAfter(LocalDateTime.now()));

    ArgumentCaptor<PageLock> captor = ArgumentCaptor.forClass(PageLock.class);
    verify(pageLockRepository).save(captor.capture());

    assertEquals(bob, captor.getValue().getOwner());
    assertTrue(LocalDateTime.now().isBefore(captor.getValue().getLockTime()));
  }

  @Test
  void test_releasePageLock() {
    when(siteService.getSiteForHostname("host")).thenReturn("site1");
    User user = new User("Bob", "pass");
    when(userService.getUser("Bob")).thenReturn(user);
    underTest.releasePageLock("host", "ns:page1", "lockId", "Bob");

    verify(pageLockRepository)
        .deleteBySiteAndNamespaceAndPagenameAndLockIdAndOwner(
            eq("site1"), eq("ns"), eq("page1"), eq("lockId"), any(User.class));
  }

  @Test
  void test_releaseAnyPageLock() {
    when(siteService.getSiteForHostname("host")).thenReturn("site1");
    underTest.releaseAnyPageLock("host", "ns:page1");

    verify(pageLockRepository).deleteBySiteAndNamespaceAndPagename("site1", "ns", "page1");
  }
}
