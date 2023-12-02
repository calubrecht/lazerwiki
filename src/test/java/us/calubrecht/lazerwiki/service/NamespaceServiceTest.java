package us.calubrecht.lazerwiki.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.model.*;
import us.calubrecht.lazerwiki.repository.NamespaceRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {NamespaceService.class})
@ActiveProfiles("test")
public class NamespaceServiceTest {
    @Autowired
    NamespaceService underTest;

    @MockBean
    NamespaceRepository namespaceRepository;

    @MockBean
    UserService userService;

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

    /**
     *   public List<PageDesc> filterReadablePages(List<PageDesc> allValid, String site, String userName) {
     *         Set<String> unreadableNamespaces = allValid.stream().map(p -> p.getNamespace()).distinct().
     *                 filter(ns -> !canReadNamespace(site, ns, userName)).collect(Collectors.toSet());
     *         return allValid.stream().filter(p -> !unreadableNamespaces.contains(p.getNamespace())).toList();
     *     }
     *
     *     public List<MediaRecord> filterReadableMedia(List<MediaRecord> allValid, String site, String userName) {
     *         List vv2 = allValid;
     *         Set<String> unreadableNamespaces = allValid.stream().map(m -> m.getNamespace()).distinct().
     *                 filter(ns -> !canReadNamespace(site, ns, userName)).collect(Collectors.toSet());
     *         return allValid.stream().filter(m -> !unreadableNamespaces.contains(m.getNamespace())).toList();
     *     }
     */

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

        MediaRecord m1 = new MediaRecord("file1", "site1", "ns1", "bob", 0, 0 ,0);
        MediaRecord m2 = new MediaRecord("file2", "site1", "closed", "bob", 0, 0 ,0);
        List<MediaRecord> allMedia = List.of(m1, m2);

        List<MediaRecord> filtered = underTest.filterReadableMedia(
                allMedia, "site1", "bob");
        assertEquals(2, filtered.size());
        filtered = underTest.filterReadableMedia(
                allMedia, "site1", null);
        assertEquals(1, filtered.size());
        assertEquals("file1", filtered.get(0).getFileName());
    }
}
