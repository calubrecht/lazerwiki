package us.calubrecht.lazerwiki.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import us.calubrecht.lazerwiki.model.*;
import us.calubrecht.lazerwiki.repository.MediaRecordRepository;
import us.calubrecht.lazerwiki.repository.NamespaceRepository;
import us.calubrecht.lazerwiki.repository.PageRepository;

@SpringBootTest(classes = {NamespaceService.class})
@ActiveProfiles("test")
public class NamespaceServiceTest {
  @Autowired NamespaceService underTest;

  @MockitoBean NamespaceRepository namespaceRepository;

  @MockitoBean UserService userService;

  @MockitoBean PageRepository pageRepository;

  @MockitoBean MediaRecordRepository mediaRecordRepository;

  @Test
  public void test_canReadNamespace() {
    assertTrue(underTest.canReadNamespace("site1", "ns_unknown", "bob"));
    assertTrue(underTest.canReadNamespace("site1", "ns_unknown", "Guest"));

    Namespace ns1 = new Namespace();
    ns1.restrictionType = Namespace.RestrictionType.OPEN;
    when(namespaceRepository.findBySiteAndNamespace("site1", "ns1")).thenReturn(ns1);
    assertTrue(underTest.canReadNamespace("site1", "ns1", "bob"));

    Namespace nsClosed = new Namespace();
    nsClosed.setNamespace("closed");
    nsClosed.restrictionType = Namespace.RestrictionType.READ_RESTRICTED;
    when(namespaceRepository.findBySiteAndNamespace("site1", "closed")).thenReturn(nsClosed);

    assertFalse(underTest.canReadNamespace("site1", "closed", "Guest"));

    User globalAdmin = new User();
    globalAdmin.roles = List.of(new UserRole(globalAdmin, "ROLE_ADMIN"));
    when(userService.getUser("bob")).thenReturn(globalAdmin);
    User otherSiteAdmin = new User();
    otherSiteAdmin.roles = List.of(new UserRole(otherSiteAdmin, "ROLE_ADMIN:other"));
    when(userService.getUser("otherSite")).thenReturn(otherSiteAdmin);
    User siteAdmin = new User();
    siteAdmin.roles = List.of(new UserRole(siteAdmin, "ROLE_ADMIN:site1"));
    when(userService.getUser("site")).thenReturn(siteAdmin);
    User readableUser = new User();
    readableUser.roles = List.of(new UserRole(readableUser, "ROLE_READ:site1:closed"));
    when(userService.getUser("readable")).thenReturn(readableUser);
    User otherUser = new User();
    otherUser.roles = List.of(new UserRole(otherUser, "ROLE_USER"));
    when(userService.getUser("other")).thenReturn(otherUser);

    assertTrue(underTest.canReadNamespace("site1", "closed", "bob"));
    assertFalse(underTest.canReadNamespace("site1", "closed", "otherSite"));
    assertTrue(underTest.canReadNamespace("site1", "closed", "site"));
    assertTrue(underTest.canReadNamespace("site1", "closed", "readable"));
    assertFalse(underTest.canReadNamespace("site1", "closed", "other"));
  }

  @Test
  public void test_permissionsAreInherited() {
    User user = new User();
    user.roles = List.of(new UserRole(user, "ROLE_USER"));
    when(userService.getUser("user")).thenReturn(user);

    Namespace ns1 = new Namespace();
    ns1.restrictionType = Namespace.RestrictionType.OPEN;
    when(namespaceRepository.findBySiteAndNamespace("site1", "ns1")).thenReturn(ns1);
    assertTrue(underTest.canReadNamespace("site1", "ns1:subns", "user"));

    Namespace nsClosed = new Namespace();
    nsClosed.setNamespace("closed");
    nsClosed.restrictionType = Namespace.RestrictionType.READ_RESTRICTED;
    when(namespaceRepository.findBySiteAndNamespace("site1", "closed")).thenReturn(nsClosed);
    assertFalse(underTest.canReadNamespace("site1", "closed:subns", "user"));

    User readableUser = new User();
    readableUser.roles = List.of(new UserRole(readableUser, "ROLE_READ:site1:closed"));
    when(userService.getUser("readable")).thenReturn(readableUser);
    assertTrue(underTest.canReadNamespace("site1", "closed:subns", "readable"));

    Namespace nsWriteClosed = new Namespace();
    nsWriteClosed.setNamespace("writeClosed");
    nsWriteClosed.restrictionType = Namespace.RestrictionType.WRITE_RESTRICTED;
    when(namespaceRepository.findBySiteAndNamespace("site1", "writeClosed"))
        .thenReturn(nsWriteClosed);
    assertFalse(underTest.canWriteNamespace("site1", "writeClosed:subns", "user"));

    User writeableUser = new User();
    writeableUser.roles = List.of(new UserRole(writeableUser, "ROLE_READ:site1:writeClosed"));
    when(userService.getUser("writeable")).thenReturn(writeableUser);
    assertTrue(underTest.canWriteNamespace("site1", "ns_writeClosed:subns", "writeable"));
  }

  @Test
  public void test_canWriteNamespace() {
    assertTrue(underTest.canWriteNamespace("site1", "ns_unknown", "bob"));
    assertFalse(underTest.canWriteNamespace("site1", "ns_unknown", "Guest"));

    Namespace ns1 = new Namespace();
    ns1.restrictionType = Namespace.RestrictionType.OPEN;
    when(namespaceRepository.findBySiteAndNamespace("site1", "ns1")).thenReturn(ns1);
    assertTrue(underTest.canWriteNamespace("site1", "ns1", "bob"));

    Namespace nsClosed = new Namespace();
    nsClosed.setNamespace("closed");
    nsClosed.restrictionType = Namespace.RestrictionType.WRITE_RESTRICTED;
    when(namespaceRepository.findBySiteAndNamespace("site1", "closed")).thenReturn(nsClosed);

    User globalAdmin = new User();
    globalAdmin.roles = List.of(new UserRole(globalAdmin, "ROLE_ADMIN"));
    when(userService.getUser("bob")).thenReturn(globalAdmin);
    User otherSiteAdmin = new User();
    otherSiteAdmin.roles = List.of(new UserRole(otherSiteAdmin, "ROLE_ADMIN:other"));
    when(userService.getUser("otherSite")).thenReturn(otherSiteAdmin);
    User siteAdmin = new User();
    siteAdmin.roles = List.of(new UserRole(siteAdmin, "ROLE_ADMIN:site1"));
    when(userService.getUser("site")).thenReturn(siteAdmin);
    User readableUser = new User();
    readableUser.roles = List.of(new UserRole(readableUser, "ROLE_WRITE:site1:closed"));
    when(userService.getUser("readable")).thenReturn(readableUser);
    User otherUser = new User();
    otherUser.roles = List.of(new UserRole(otherUser, "ROLE_USER"));
    when(userService.getUser("other")).thenReturn(otherUser);

    assertTrue(underTest.canWriteNamespace("site1", "closed", "bob"));
    assertFalse(underTest.canWriteNamespace("site1", "closed", "otherSite"));
    assertTrue(underTest.canWriteNamespace("site1", "closed", "site"));
    assertTrue(underTest.canWriteNamespace("site1", "closed", "readable"));
    assertFalse(underTest.canWriteNamespace("site1", "closed", "other"));
  }

  @Test
  public void test_filterReadablePages() {
    Namespace ns1 = new Namespace();
    ns1.restrictionType = Namespace.RestrictionType.OPEN;
    when(namespaceRepository.findBySiteAndNamespace("site1", "ns1")).thenReturn(ns1);
    Namespace nsClosed = new Namespace();
    nsClosed.setNamespace("closed");
    nsClosed.restrictionType = Namespace.RestrictionType.READ_RESTRICTED;
    when(namespaceRepository.findBySiteAndNamespace("site1", "closed")).thenReturn(nsClosed);

    User globalAdmin = new User();
    globalAdmin.roles = List.of(new UserRole(globalAdmin, "ROLE_ADMIN"));
    when(userService.getUser("bob")).thenReturn(globalAdmin);

    PageDesc page1 = new PageServiceTest.PageDescImpl("ns1", "blue", "", "");
    PageDesc page2 = new PageServiceTest.PageDescImpl("closed", "secret", "", "");
    List<PageDesc> allPages = List.of(page1, page2);

    List<PageDesc> filtered = underTest.filterReadablePages(allPages, "site1", "bob");
    assertEquals(2, filtered.size());
    filtered = underTest.filterReadablePages(allPages, "site1", null);
    assertEquals(1, filtered.size());
    assertEquals("blue", filtered.get(0).getPagename());
  }

  @Test
  public void test_filterReadablePageDescriptors() {
    Namespace ns1 = new Namespace();
    ns1.restrictionType = Namespace.RestrictionType.OPEN;
    when(namespaceRepository.findBySiteAndNamespace("site1", "ns1")).thenReturn(ns1);
    Namespace nsClosed = new Namespace();
    nsClosed.setNamespace("closed");
    nsClosed.restrictionType = Namespace.RestrictionType.READ_RESTRICTED;
    when(namespaceRepository.findBySiteAndNamespace("site1", "closed")).thenReturn(nsClosed);

    User globalAdmin = new User();
    globalAdmin.roles = List.of(new UserRole(globalAdmin, "ROLE_ADMIN"));
    when(userService.getUser("bob")).thenReturn(globalAdmin);

    PageDescriptor page1 = new PageDescriptor("ns1", "blue");
    PageDescriptor page2 = new PageDescriptor("closed", "secret");
    List<PageDescriptor> allPages = List.of(page1, page2);

    List<PageDescriptor> filtered =
        underTest.filterReadablePageDescriptors(allPages, "site1", "bob");
    assertEquals(2, filtered.size());
    filtered = underTest.filterReadablePageDescriptors(allPages, "site1", null);
    assertEquals(1, filtered.size());
    assertEquals("blue", filtered.get(0).pageName());
  }

  @Test
  public void test_filterMedia() {
    Namespace ns1 = new Namespace();
    ns1.restrictionType = Namespace.RestrictionType.OPEN;
    when(namespaceRepository.findBySiteAndNamespace("site1", "ns1")).thenReturn(ns1);
    Namespace nsClosed = new Namespace();
    nsClosed.setNamespace("closed");
    nsClosed.restrictionType = Namespace.RestrictionType.READ_RESTRICTED;
    when(namespaceRepository.findBySiteAndNamespace("site1", "closed")).thenReturn(nsClosed);

    User globalAdmin = new User();
    globalAdmin.roles = List.of(new UserRole(globalAdmin, "ROLE_ADMIN"));
    when(userService.getUser("bob")).thenReturn(globalAdmin);

    MediaRecord m1 = new MediaRecord("file1", "site1", "ns1", globalAdmin, 0, 0, 0);
    MediaRecord m2 = new MediaRecord("file2", "site1", "closed", globalAdmin, 0, 0, 0);
    List<MediaRecord> allMedia = List.of(m1, m2);

    List<MediaRecord> filtered = underTest.filterReadableMedia(allMedia, "site1", "bob");
    assertEquals(2, filtered.size());
    filtered = underTest.filterReadableMedia(allMedia, "site1", null);
    assertEquals(1, filtered.size());
    assertEquals("file1", filtered.get(0).getFileName());
  }

  @Test
  public void test_canUploadInNamespace() {
    assertFalse(underTest.canUploadInNamespace("site1", "ns_unknown", "Guest"));
    Namespace nsClosed = new Namespace();
    nsClosed.setNamespace("closed");
    nsClosed.restrictionType = Namespace.RestrictionType.WRITE_RESTRICTED;
    when(namespaceRepository.findBySiteAndNamespace("site1", "closed")).thenReturn(nsClosed);
    User uploadUser = new User();
    uploadUser.roles = List.of(new UserRole(uploadUser, "ROLE_UPLOAD:site1"));
    when(userService.getUser("uploadUser")).thenReturn(uploadUser);
    User normUser = new User();
    normUser.roles = List.of(new UserRole(normUser, "ROLE_USER"));
    when(userService.getUser("normUser")).thenReturn(normUser);

    assertTrue(underTest.canUploadInNamespace("site1", "ns_open", "uploadUser"));
    assertFalse(underTest.canUploadInNamespace("site1", "closed", "uploadUser"));
    assertFalse(underTest.canUploadInNamespace("site1", "ns_open", "normUser"));
  }

  @Test
  public void test_canDeleteInNamespace() {
    assertFalse(underTest.canDeleteInNamespace("site1", "ns_unknown", "Guest"));
    Namespace nsClosed = new Namespace();
    nsClosed.setNamespace("closed");
    nsClosed.restrictionType = Namespace.RestrictionType.WRITE_RESTRICTED;
    when(namespaceRepository.findBySiteAndNamespace("site1", "closed")).thenReturn(nsClosed);
    User uploadUser = new User();
    uploadUser.roles = List.of(new UserRole(uploadUser, "ROLE_DELETE:site1"));
    when(userService.getUser("uploadUser")).thenReturn(uploadUser);
    User normUser = new User();
    normUser.roles = List.of(new UserRole(normUser, "ROLE_USER"));
    when(userService.getUser("normUser")).thenReturn(normUser);

    assertTrue(underTest.canDeleteInNamespace("site1", "ns_open", "uploadUser"));
    assertFalse(underTest.canDeleteInNamespace("site1", "closed", "uploadUser"));
    assertFalse(underTest.canDeleteInNamespace("site1", "ns_open", "normUser"));
  }

  @Test
  public void test_parentNamespace() {
    assertEquals(null, underTest.parentNamespace(""));
    assertEquals("", underTest.parentNamespace("ns"));
    assertEquals("ns", underTest.parentNamespace("ns:n2"));
    assertEquals("ns:n2", underTest.parentNamespace("ns:n2:n5"));
  }

  @Test
  public void test_readableNamespaces() {
    when(pageRepository.getAllNamespaces(eq("site1"))).thenReturn(List.of("ns1", "ns2", "ns3"));
    Namespace nsClosed = new Namespace();
    nsClosed.setNamespace("ns3");
    nsClosed.restrictionType = Namespace.RestrictionType.READ_RESTRICTED;
    when(namespaceRepository.findBySiteAndNamespace("site1", "ns3")).thenReturn(nsClosed);
    User bob = new User("Bob", "");
    bob.roles = new ArrayList<>();
    when(userService.getUser("Bob")).thenReturn(bob);
    List<String> namespaces = underTest.getReadableNamespaces("site1", "Bob");

    assertEquals(List.of("ns1", "ns2"), namespaces);
  }

  @Test
  public void test_getNSRestriction() {
    Namespace nsObj = new Namespace();
    nsObj.setSite("site1");
    nsObj.setNamespace("ns1");
    nsObj.restrictionType = Namespace.RestrictionType.READ_RESTRICTED;
    when(namespaceRepository.findBySiteAndNamespace("site1", "ns1")).thenReturn(nsObj);

    assertEquals(
        Namespace.RestrictionType.READ_RESTRICTED, underTest.getNSRestriction("site1", "ns1"));
    assertEquals(Namespace.RestrictionType.INHERIT, underTest.getNSRestriction("site1", "ns2"));
  }

  @Test
  public void test_setNSRestriction() {
    Namespace nsObj = new Namespace();
    nsObj.id = 1L;
    nsObj.setSite("site1");
    nsObj.setNamespace("ns1");
    nsObj.restrictionType = Namespace.RestrictionType.READ_RESTRICTED;
    when(namespaceRepository.findBySiteAndNamespace("site1", "ns1")).thenReturn(nsObj);

    underTest.setNSRestriction("site1", "ns1", Namespace.RestrictionType.WRITE_RESTRICTED);
    underTest.setNSRestriction("site1", "ns2", Namespace.RestrictionType.READ_RESTRICTED);

    ArgumentCaptor<Namespace> nsCaptor = ArgumentCaptor.forClass(Namespace.class);
    verify(namespaceRepository, times(2)).save(nsCaptor.capture());

    assertEquals(1L, nsCaptor.getAllValues().get(0).id);
    assertEquals("ns1", nsCaptor.getAllValues().get(0).getNamespace());
    assertEquals(
        Namespace.RestrictionType.WRITE_RESTRICTED, nsCaptor.getAllValues().get(0).restrictionType);
    assertEquals("ns2", nsCaptor.getAllValues().get(1).getNamespace());
    assertEquals(
        Namespace.RestrictionType.READ_RESTRICTED, nsCaptor.getAllValues().get(1).restrictionType);

    Namespace nsObj3 = new Namespace();
    nsObj3.id = 5L;
    nsObj3.setSite("site1");
    nsObj3.setNamespace("ns3");
    nsObj3.restrictionType = Namespace.RestrictionType.READ_RESTRICTED;
    when(namespaceRepository.findBySiteAndNamespace("site1", "ns3")).thenReturn(nsObj3);

    underTest.setNSRestriction("site1", "ns3", Namespace.RestrictionType.INHERIT);
    verify(namespaceRepository).delete(nsObj3);

    underTest.setNSRestriction("site1", "ns4", Namespace.RestrictionType.INHERIT);
    // Delete not called for ns4
    verify(namespaceRepository, times(1)).delete(any());
  }

  @Test
  public void test_joinNS() {
    assertEquals("singleNS", underTest.joinNS("", "singleNS"));
    assertEquals("ns:nestedNS", underTest.joinNS("ns", "nestedNS"));
  }

  @Test
  public void test_canWriteGuest() {
    Namespace nsObj3 = new Namespace();
    nsObj3.id = 5L;
    nsObj3.setSite("site1");
    nsObj3.setNamespace("ns3");
    nsObj3.restrictionType = Namespace.RestrictionType.OPEN;

    when(namespaceRepository.findBySiteAndNamespace("site1", "ns3")).thenReturn(nsObj3);
    Namespace nsObjGuestWritable = new Namespace();
    nsObjGuestWritable.id = 5L;
    nsObjGuestWritable.setSite("site1");
    nsObjGuestWritable.setNamespace("guestWritable");
    nsObjGuestWritable.restrictionType = Namespace.RestrictionType.GUEST_WRITABLE;

    when(namespaceRepository.findBySiteAndNamespace("site1", "guestWritable"))
        .thenReturn(nsObjGuestWritable);
    assertFalse(underTest.canWriteNamespace("site1", "ns3", null));
    assertFalse(underTest.canWriteNamespace("site1", "basicNS", null));
    assertTrue(underTest.canWriteNamespace("site1", "guestWritable", null));

    assertTrue(underTest.canReadNamespace("site1", "guestWritable", null));
  }
}
