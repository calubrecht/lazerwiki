package us.calubrecht.lazerwiki.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.model.*;
import us.calubrecht.lazerwiki.repository.MediaRecordRepository;
import us.calubrecht.lazerwiki.repository.NamespaceRepository;
import us.calubrecht.lazerwiki.repository.PageRepository;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {NamespaceService.class})
@ActiveProfiles("test")
public class NamespaceServiceTest {
    @Autowired
    NamespaceService underTest;

    @MockBean
    NamespaceRepository namespaceRepository;

    @MockBean
    UserService userService;

    @MockBean
    PageRepository pageRepository;

    @MockBean
    MediaRecordRepository mediaRecordRepository;

    @Test
    public void testCanReadNamespace() {
        assertTrue(underTest.canReadNamespace("site1", "ns_unknown", "bob"));
        assertTrue(underTest.canReadNamespace("site1", "ns_unknown", "Guest"));

        Namespace ns1 = new Namespace();
        ns1.restriction_type = Namespace.RESTRICTION_TYPE.OPEN;
        when(namespaceRepository.findBySiteAndNamespace("site1", "ns1")).thenReturn(ns1);
        assertTrue(underTest.canReadNamespace("site1", "ns1", "bob"));

        Namespace ns_closed = new Namespace();
        ns_closed.namespace="closed";
        ns_closed.restriction_type = Namespace.RESTRICTION_TYPE.READ_RESTRICTED;
        when(namespaceRepository.findBySiteAndNamespace("site1", "closed")).thenReturn(ns_closed);

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
    public void permissionsAreInherited() {
        User user = new User();
        user.roles = List.of(new UserRole(user, "ROLE_USER"));
        when(userService.getUser("user")).thenReturn(user);

        Namespace ns1 = new Namespace();
        ns1.restriction_type = Namespace.RESTRICTION_TYPE.OPEN;
        when(namespaceRepository.findBySiteAndNamespace("site1", "ns1")).thenReturn(ns1);
        assertTrue(underTest.canReadNamespace("site1", "ns1:subns", "user"));

        Namespace ns_closed = new Namespace();
        ns_closed.namespace="closed";
        ns_closed.restriction_type = Namespace.RESTRICTION_TYPE.READ_RESTRICTED;
        when(namespaceRepository.findBySiteAndNamespace("site1", "closed")).thenReturn(ns_closed);
        assertFalse(underTest.canReadNamespace("site1", "closed:subns", "user"));

        User readableUser = new User();
        readableUser.roles = List.of(new UserRole(readableUser, "ROLE_READ:site1:closed"));
        when(userService.getUser("readable")).thenReturn(readableUser);
        assertTrue(underTest.canReadNamespace("site1", "closed:subns", "readable"));

        Namespace ns_writeClosed = new Namespace();
        ns_writeClosed.namespace="writeClosed";
        ns_writeClosed.restriction_type = Namespace.RESTRICTION_TYPE.WRITE_RESTRICTED;
        when(namespaceRepository.findBySiteAndNamespace("site1", "writeClosed")).thenReturn(ns_writeClosed);
        assertFalse(underTest.canWriteNamespace("site1", "writeClosed:subns", "user"));

        User writeableUser = new User();
        writeableUser.roles = List.of(new UserRole(writeableUser, "ROLE_READ:site1:writeClosed"));
        when(userService.getUser("writeable")).thenReturn(writeableUser);
        assertTrue(underTest.canWriteNamespace("site1", "ns_writeClosed:subns", "writeable"));
    }


    @Test
    public void testCanWriteNamespace() {
        assertTrue(underTest.canWriteNamespace("site1", "ns_unknown", "bob"));
        assertFalse(underTest.canWriteNamespace("site1", "ns_unknown", "Guest"));

        Namespace ns1 = new Namespace();
        ns1.restriction_type = Namespace.RESTRICTION_TYPE.OPEN;
        when(namespaceRepository.findBySiteAndNamespace("site1", "ns1")).thenReturn(ns1);
        assertTrue(underTest.canWriteNamespace("site1", "ns1", "bob"));

        Namespace ns_closed = new Namespace();
        ns_closed.namespace="closed";
        ns_closed.restriction_type = Namespace.RESTRICTION_TYPE.WRITE_RESTRICTED;
        when(namespaceRepository.findBySiteAndNamespace("site1", "closed")).thenReturn(ns_closed);

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
        ns1.restriction_type = Namespace.RESTRICTION_TYPE.OPEN;
        when(namespaceRepository.findBySiteAndNamespace("site1", "ns1")).thenReturn(ns1);
        Namespace ns_closed = new Namespace();
        ns_closed.namespace="closed";
        ns_closed.restriction_type = Namespace.RESTRICTION_TYPE.READ_RESTRICTED;
        when(namespaceRepository.findBySiteAndNamespace("site1", "closed")).thenReturn(ns_closed);

        User globalAdmin = new User();
        globalAdmin.roles = List.of(new UserRole(globalAdmin, "ROLE_ADMIN"));
        when(userService.getUser("bob")).thenReturn(globalAdmin);

        PageDesc Page1 = new PageServiceTest.PageDescImpl("ns1", "blue", "", "");
        PageDesc Page2 = new PageServiceTest.PageDescImpl("closed", "secret", "", "");
        List<PageDesc> allPages = List.of(Page1, Page2);

        List<PageDesc> filtered = underTest.filterReadablePages(
                allPages, "site1", "bob");
        assertEquals(2, filtered.size());
        filtered = underTest.filterReadablePages(
                allPages, "site1", null);
        assertEquals(1, filtered.size());
        assertEquals("blue", filtered.get(0).getPagename());
    }

    @Test
    public void test_filterReadablePageDescriptors() {
        Namespace ns1 = new Namespace();
        ns1.restriction_type = Namespace.RESTRICTION_TYPE.OPEN;
        when(namespaceRepository.findBySiteAndNamespace("site1", "ns1")).thenReturn(ns1);
        Namespace ns_closed = new Namespace();
        ns_closed.namespace="closed";
        ns_closed.restriction_type = Namespace.RESTRICTION_TYPE.READ_RESTRICTED;
        when(namespaceRepository.findBySiteAndNamespace("site1", "closed")).thenReturn(ns_closed);

        User globalAdmin = new User();
        globalAdmin.roles = List.of(new UserRole(globalAdmin, "ROLE_ADMIN"));
        when(userService.getUser("bob")).thenReturn(globalAdmin);

        PageDescriptor Page1 = new PageDescriptor("ns1", "blue");
        PageDescriptor Page2 = new PageDescriptor("closed", "secret");
        List<PageDescriptor> allPages = List.of(Page1, Page2);

        List<PageDescriptor> filtered = underTest.filterReadablePageDescriptors(
                allPages, "site1", "bob");
        assertEquals(2, filtered.size());
        filtered = underTest.filterReadablePageDescriptors(
                allPages, "site1", null);
        assertEquals(1, filtered.size());
        assertEquals("blue", filtered.get(0).pageName());
    }


    @Test
    public void test_filterMedia() {
        Namespace ns1 = new Namespace();
        ns1.restriction_type = Namespace.RESTRICTION_TYPE.OPEN;
        when(namespaceRepository.findBySiteAndNamespace("site1", "ns1")).thenReturn(ns1);
        Namespace ns_closed = new Namespace();
        ns_closed.namespace="closed";
        ns_closed.restriction_type = Namespace.RESTRICTION_TYPE.READ_RESTRICTED;
        when(namespaceRepository.findBySiteAndNamespace("site1", "closed")).thenReturn(ns_closed);

        User globalAdmin = new User();
        globalAdmin.roles = List.of(new UserRole(globalAdmin, "ROLE_ADMIN"));
        when(userService.getUser("bob")).thenReturn(globalAdmin);

        MediaRecord m1 = new MediaRecord("file1", "site1", "ns1", globalAdmin, 0, 0 ,0);
        MediaRecord m2 = new MediaRecord("file2", "site1", "closed", globalAdmin, 0, 0 ,0);
        List<MediaRecord> allMedia = List.of(m1, m2);

        List<MediaRecord> filtered = underTest.filterReadableMedia(
                allMedia, "site1", "bob");
        assertEquals(2, filtered.size());
        filtered = underTest.filterReadableMedia(
                allMedia, "site1", null);
        assertEquals(1, filtered.size());
        assertEquals("file1", filtered.get(0).getFileName());
    }

    @Test
    public void testCanUploadInNamespace() {
        assertFalse(underTest.canUploadInNamespace("site1", "ns_unknown", "Guest"));
        Namespace ns_closed = new Namespace();
        ns_closed.namespace="closed";
        ns_closed.restriction_type = Namespace.RESTRICTION_TYPE.WRITE_RESTRICTED;
        when(namespaceRepository.findBySiteAndNamespace("site1", "closed")).thenReturn(ns_closed);
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
    public void testCanDeleteInNamespace() {
        assertFalse(underTest.canDeleteInNamespace("site1", "ns_unknown", "Guest"));
        Namespace ns_closed = new Namespace();
        ns_closed.namespace="closed";
        ns_closed.restriction_type = Namespace.RESTRICTION_TYPE.WRITE_RESTRICTED;
        when(namespaceRepository.findBySiteAndNamespace("site1", "closed")).thenReturn(ns_closed);
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
    public void testParentNamespace() {
        assertEquals(null, underTest.parentNamespace(""));
        assertEquals("", underTest.parentNamespace("ns"));
        assertEquals("ns", underTest.parentNamespace("ns:n2"));
        assertEquals("ns:n2", underTest.parentNamespace("ns:n2:n5"));
    }

    @Test
    public void testReadableNamespaces() {
        when(pageRepository.getAllNamespaces(eq("site1"))).thenReturn(List.of("ns1", "ns2", "ns3"));
        Namespace ns_closed = new Namespace();
        ns_closed.namespace="ns3";
        ns_closed.restriction_type = Namespace.RESTRICTION_TYPE.READ_RESTRICTED;
        when(namespaceRepository.findBySiteAndNamespace("site1", "ns3")).thenReturn(ns_closed);
        User bob = new User("Bob", "");
        bob.roles = new ArrayList<>();
        when(userService.getUser("Bob")).thenReturn(bob);
        List<String> namespaces = underTest.getReadableNamespaces("site1", "Bob");

        assertEquals(List.of("ns1", "ns2"), namespaces);
    }

    @Test
    public void testGetNSRestriction() {
        Namespace nsObj = new Namespace();
        nsObj.site = "site1";
        nsObj.namespace ="ns1";
        nsObj.restriction_type = Namespace.RESTRICTION_TYPE.READ_RESTRICTED;
        when(namespaceRepository.findBySiteAndNamespace("site1", "ns1")).thenReturn(nsObj);

        assertEquals(Namespace.RESTRICTION_TYPE.READ_RESTRICTED, underTest.getNSRestriction("site1", "ns1"));
        assertEquals(Namespace.RESTRICTION_TYPE.INHERIT, underTest.getNSRestriction("site1", "ns2"));
    }

    @Test
    public void testSetNSRestriction() {
        Namespace nsObj = new Namespace();
        nsObj.id = 1L;
        nsObj.site = "site1";
        nsObj.namespace ="ns1";
        nsObj.restriction_type = Namespace.RESTRICTION_TYPE.READ_RESTRICTED;
        when(namespaceRepository.findBySiteAndNamespace("site1", "ns1")).thenReturn(nsObj);

        underTest.setNSRestriction("site1", "ns1", Namespace.RESTRICTION_TYPE.WRITE_RESTRICTED);
        underTest.setNSRestriction("site1", "ns2", Namespace.RESTRICTION_TYPE.READ_RESTRICTED);

        ArgumentCaptor<Namespace> nsCaptor = ArgumentCaptor.forClass(Namespace.class);
        verify(namespaceRepository, times(2)).save(nsCaptor.capture());

        assertEquals(1L, nsCaptor.getAllValues().get(0).id);
        assertEquals("ns1", nsCaptor.getAllValues().get(0).namespace);
        assertEquals(Namespace.RESTRICTION_TYPE.WRITE_RESTRICTED, nsCaptor.getAllValues().get(0).restriction_type);
        assertEquals("ns2", nsCaptor.getAllValues().get(1).namespace);
        assertEquals(Namespace.RESTRICTION_TYPE.READ_RESTRICTED, nsCaptor.getAllValues().get(1).restriction_type);

        Namespace nsObj3 = new Namespace();
        nsObj3.id = 5L;
        nsObj3.site = "site1";
        nsObj3.namespace ="ns3";
        nsObj3.restriction_type = Namespace.RESTRICTION_TYPE.READ_RESTRICTED;
        when(namespaceRepository.findBySiteAndNamespace("site1", "ns3")).thenReturn(nsObj3);

        underTest.setNSRestriction("site1", "ns3", Namespace.RESTRICTION_TYPE.INHERIT);
        verify(namespaceRepository).delete(nsObj3);

        underTest.setNSRestriction("site1", "ns4", Namespace.RESTRICTION_TYPE.INHERIT);
        // Delete not called for ns4
        verify(namespaceRepository, times(1)).delete(any());
    }

    @Test
    public void testJoinNS() {
        assertEquals("singleNS", underTest.joinNS("", "singleNS"));
        assertEquals("ns:nestedNS", underTest.joinNS("ns", "nestedNS"));
    }
}
