package us.calubrecht.lazerwiki.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import us.calubrecht.lazerwiki.model.Site;
import us.calubrecht.lazerwiki.model.User;
import us.calubrecht.lazerwiki.model.UserDTO;
import us.calubrecht.lazerwiki.model.UserRole;
import us.calubrecht.lazerwiki.requests.SiteSettingsRequest;
import us.calubrecht.lazerwiki.service.*;
import us.calubrecht.lazerwiki.service.exception.SiteSettingsException;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AdminController.class, VersionController.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    UserService userService;

    @MockBean
    SiteService siteService;

    @MockBean
    RegenCacheService regenCacheService;

    @MockBean
    PageUpdateService pageUpdateService;

    @MockBean
    SiteDelService siteDelService;

    @Test
    void regenLinkTable() throws Exception {
        User adminUser = new User();
        adminUser.roles = List.of(new UserRole(adminUser, "ROLE_ADMIN"));
        User siteAdmin = new User();
        siteAdmin.roles = List.of(new UserRole(siteAdmin, "ROLE_ADMIN:default"));
        User regularUser = new User();
        regularUser.roles = List.of(new UserRole(regularUser, "ROLE_USER"));
        when(userService.getUser("bob")).thenReturn(adminUser);
        when(userService.getUser("celia")).thenReturn(siteAdmin);
        when(userService.getUser("frank")).thenReturn(regularUser);

        this.mockMvc.perform(post("/api/admin/regenLinkTable/default").principal(new UsernamePasswordAuthenticationToken("bob", ""))).
                andExpect(status().isOk());
        this.mockMvc.perform(post("/api/admin/regenLinkTable/default").principal(new UsernamePasswordAuthenticationToken("celia", ""))).
                andExpect(status().isOk());
        verify(regenCacheService, times(2)).regenLinks("default");
        this.mockMvc.perform(post("/api/admin/regenLinkTable/default").principal(new UsernamePasswordAuthenticationToken("frank", ""))).
                andExpect(status().isUnauthorized());
        verify(regenCacheService, times(2)).regenLinks("default");
    }

    @Test
    void regenCacheTable() throws Exception {
        User adminUser = new User();
        adminUser.roles = List.of(new UserRole(adminUser, "ROLE_ADMIN"));
        User siteAdmin = new User();
        siteAdmin.roles = List.of(new UserRole(siteAdmin, "ROLE_ADMIN:default"));
        User regularUser = new User();
        regularUser.roles = List.of(new UserRole(regularUser, "ROLE_USER"));
        when(userService.getUser("bob")).thenReturn(adminUser);
        when(userService.getUser("celia")).thenReturn(siteAdmin);
        when(userService.getUser("frank")).thenReturn(regularUser);

        this.mockMvc.perform(post("/api/admin/regenCacheTable/default").principal(new UsernamePasswordAuthenticationToken("bob", ""))).
                andExpect(status().isOk());
        this.mockMvc.perform(post("/api/admin/regenCacheTable/default").principal(new UsernamePasswordAuthenticationToken("celia", ""))).
                andExpect(status().isOk());
        verify(regenCacheService, times(2)).regenCache("default");
        this.mockMvc.perform(post("/api/admin/regenCacheTable/default").principal(new UsernamePasswordAuthenticationToken("frank", ""))).
                andExpect(status().isUnauthorized());
        verify(regenCacheService, times(2)).regenCache("default");
    }

    @Test
    void getUsers() throws Exception {
        when(userService.getUsers()).thenReturn(List.of(new UserDTO("Bob",null, List.of("ROLE_ADMIN","ROLE_USER")), new UserDTO("Frank", null, List.of("ROLE_USER"))));
        User adminUser = new User();
        adminUser.roles = List.of(new UserRole(adminUser, "ROLE_ADMIN"));
        when(userService.getUser("bob")).thenReturn(adminUser);
        User regularUser = new User();
        regularUser.roles = List.of(new UserRole(regularUser, "ROLE_USER"));
        when(userService.getUser("frank")).thenReturn(regularUser);

        this.mockMvc.perform(get("/api/admin/getUsers").principal(new UsernamePasswordAuthenticationToken("bob", ""))).
        andExpect(status().isOk()).andExpect(content().json("[{\"userName\":\"Bob\", \"userRoles\":[\"ROLE_ADMIN\", \"ROLE_USER\"]}, {\"userName\":\"Frank\", \"userRoles\":[\"ROLE_USER\"]}]"));

        this.mockMvc.perform(get("/api/admin/getUsers").principal(new UsernamePasswordAuthenticationToken("frank", ""))).
                andExpect(status().isUnauthorized());
    }

    @Test
    void deleteRole() throws Exception {
        when(userService.deleteRole(eq("Frank"), eq("ROLE_ADMIN"))).thenReturn(new UserDTO("Frank",null, List.of("ROLE_USER")));
        when(userService.deleteRole(eq("Bob"), eq("ROLE_EXTRA"))).thenReturn(new UserDTO("Bob",null, List.of("ROLE_ADMIN")));
        User adminUser = new User();
        adminUser.roles = List.of(new UserRole(adminUser, "ROLE_ADMIN"));
        adminUser.userName = "Bob";
        when(userService.getUser("Bob")).thenReturn(adminUser);
        User regularUser = new User();
        regularUser.roles = List.of(new UserRole(adminUser, "ROLE_USER"));
        regularUser.userName = "Frank";
        when(userService.getUser("Frank")).thenReturn(regularUser);

        this.mockMvc.perform(delete("/api/admin/role/Frank/ROLE_ADMIN").principal(new UsernamePasswordAuthenticationToken("Bob", ""))).
                andExpect(status().isOk()).andExpect(content().json("{\"userName\":\"Frank\", \"userRoles\":[\"ROLE_USER\"]}"));

        // Cannot delete own ADMIN
        this.mockMvc.perform(delete("/api/admin/role/Bob/ROLE_ADMIN").principal(new UsernamePasswordAuthenticationToken("Bob", ""))).
                andExpect(status().isUnauthorized());

        // But can delete other own roles
        this.mockMvc.perform(delete("/api/admin/role/Bob/ROLE_EXTRA").principal(new UsernamePasswordAuthenticationToken("Bob", ""))).
                andExpect(status().isOk()).andExpect(content().json("{\"userName\":\"Bob\", \"userRoles\":[\"ROLE_ADMIN\"]}"));

        // Only Admin can delete
        this.mockMvc.perform(delete("/api/admin/role/Frank/ROLE_EXTRA").principal(new UsernamePasswordAuthenticationToken("Frank", ""))).
                andExpect(status().isUnauthorized());
    }

    @Test
    void addRoles() throws Exception {
        when(userService.addRole(eq("Frank"), eq("ROLE_ADMIN"))).thenReturn(new UserDTO("Frank",null, List.of("ROLE_USER")));
        User adminUser = new User();
        adminUser.roles = List.of(new UserRole(adminUser, "ROLE_ADMIN"));
        adminUser.userName = "Bob";
        when(userService.getUser("Bob")).thenReturn(adminUser);
        User regularUser = new User();
        regularUser.roles = List.of(new UserRole(adminUser, "ROLE_USER"));
        regularUser.userName = "Frank";
        when(userService.getUser("Frank")).thenReturn(regularUser);

        this.mockMvc.perform(put("/api/admin/role/Frank/ROLE_ADMIN").principal(new UsernamePasswordAuthenticationToken("Bob", ""))).
                andExpect(status().isOk()).andExpect(content().json("{\"userName\":\"Frank\", \"userRoles\":[\"ROLE_USER\"]}"));

        // Only Admin can add role
        this.mockMvc.perform(put("/api/admin/role/Frank/ROLE_EXTRA").principal(new UsernamePasswordAuthenticationToken("Frank", ""))).
                andExpect(status().isUnauthorized());
    }

    @Test
    void addUser() throws Exception {
        User u =  new User("NewUser", null);
        u.roles = List.of(new UserRole(u, "ROLE_USER"));
        when(userService.getUser(eq("NewUser"))).thenReturn(null, u);
        User adminUser = new User();
        adminUser.roles = List.of(new UserRole(adminUser, "ROLE_ADMIN"));
        adminUser.userName = "Bob";
        when(userService.getUser("Bob")).thenReturn(adminUser);
        User regularUser = new User();
        regularUser.roles = List.of(new UserRole(adminUser, "ROLE_USER"));
        regularUser.userName = "Frank";
        when(userService.getUser("Frank")).thenReturn(regularUser);
        this.mockMvc.perform(put("/api/admin/user/NewUser").content("{\"userName\": \"NewUser\", \"password\": \"password\"}").contentType(MediaType.APPLICATION_JSON).principal(new UsernamePasswordAuthenticationToken("Bob", ""))).
                andExpect(status().isOk()).andExpect(content().json("{\"userName\":\"NewUser\", \"userRoles\":[\"ROLE_USER\"]}"));

        // Only Admin can add user
        this.mockMvc.perform(put("/api/admin/user/NewUser").content("{\"userName\": \"NewUser\", \"password\": \"password\"}").contentType(MediaType.APPLICATION_JSON).principal(new UsernamePasswordAuthenticationToken("Frank", ""))).
                andExpect(status().isUnauthorized());

        // Cannot add user that already exists
        this.mockMvc.perform(put("/api/admin/user/NewUser").content("{\"userName\": \"NewUser\", \"password\": \"password\"}").contentType(MediaType.APPLICATION_JSON).principal(new UsernamePasswordAuthenticationToken("Bob", ""))).
                andExpect(status().isConflict());
    }

    @Test
    void passwordReset() throws Exception {
        User u =  new User("User", null);
        u.roles = List.of(new UserRole(u, "ROLE_USER"));
        when(userService.getUser(eq("NewUser"))).thenReturn(u);
        User adminUser = new User();
        adminUser.roles = List.of(new UserRole(adminUser, "ROLE_ADMIN"));
        adminUser.userName = "Bob";
        when(userService.getUser("Bob")).thenReturn(adminUser);
        User regularUser = new User();
        regularUser.roles = List.of(new UserRole(adminUser, "ROLE_USER"));
        regularUser.userName = "Frank";
        when(userService.getUser("Frank")).thenReturn(regularUser);
        this.mockMvc.perform(post("/api/admin/passwordReset/User").content("{\"userName\": \"User\", \"password\": \"password\"}").contentType(MediaType.APPLICATION_JSON).principal(new UsernamePasswordAuthenticationToken("Bob", ""))).
                andExpect(status().isOk());

        verify(userService).resetPassword("User", "password");
        // Only Admin can add user
        this.mockMvc.perform(post("/api/admin/passwordReset/User").content("{\"userName\": \"User\", \"password\": \"password\"}").contentType(MediaType.APPLICATION_JSON).principal(new UsernamePasswordAuthenticationToken("Frank", ""))).
                andExpect(status().isUnauthorized());
        verify(userService, times(1)).resetPassword("User", "password");
    }

    @Test
    void deleteUser() throws Exception {
        User adminUser = new User();
        adminUser.roles = List.of(new UserRole(adminUser, "ROLE_ADMIN"));
        adminUser.userName = "Bob";
        when(userService.getUser("Bob")).thenReturn(adminUser);
        User regularUser = new User();
        regularUser.roles = List.of(new UserRole(adminUser, "ROLE_USER"));
        regularUser.userName = "Frank";
        when(userService.getUser("Frank")).thenReturn(regularUser);
        this.mockMvc.perform(delete("/api/admin/user/User").principal(new UsernamePasswordAuthenticationToken("Bob", ""))).
                andExpect(status().isOk());

        verify(userService).deleteUser("User");
        // Only Admin can add user
        this.mockMvc.perform(delete("/api/admin/user/User").principal(new UsernamePasswordAuthenticationToken("Frank", ""))).
                andExpect(status().isUnauthorized());
        verify(userService, times(1)).deleteUser("User");
    }

    @Test
    void getSites() throws Exception {
        User adminUser = new User();
        adminUser.roles = List.of(new UserRole(adminUser, "ROLE_ADMIN"));
        adminUser.userName = "Bob";
        when(userService.getUser("Bob")).thenReturn(adminUser);
        when(siteService.getAllSites(adminUser)).thenReturn(List.of(new Site("OneWiki", "wiki.com", "One Wiki"), new Site("TwoWiki", "wiki2.com", "Two Wiki")));
        this.mockMvc.perform(get("/api/admin/sites").principal(new UsernamePasswordAuthenticationToken("Bob", ""))).
                andExpect(status().isOk()).andExpect(content().json("[{\"name\":\"OneWiki\", \"hostname\":\"wiki.com\", \"siteName\":\"One Wiki\"},{\"name\":\"TwoWiki\"}]"));
    }

    @Test
    void addSite() throws Exception {
        User adminUser = new User();
        adminUser.roles = List.of(new UserRole(adminUser, "ROLE_ADMIN"));
        adminUser.userName = "Bob";
        when(userService.getUser("Bob")).thenReturn(adminUser);
        User regularUser = new User();
        regularUser.roles = List.of(new UserRole(adminUser, "ROLE_USER"));
        regularUser.userName = "Frank";
        when(userService.getUser("Frank")).thenReturn(regularUser);
        when(siteService.getAllSites(any())).thenReturn(List.of(new Site("OneWiki", "wiki.com", "One Wiki"), new Site("TwoWiki", "wiki2.com", "Two Wiki")));

        // No for Frank
        // {"siteName":"site1", "displayName":"Site 1", "hostName":"site.com"}
        String content = "{\"name\":\"site1\", \"siteName\":\"Site 1\", \"hostName\":\"site.com\"}";
        this.mockMvc.perform(put("/api/admin/site/site1").content(content).contentType(MediaType.APPLICATION_JSON).principal(new UsernamePasswordAuthenticationToken("Frank", ""))).
                andExpect(status().isUnauthorized());

        // Creat site fails, don't go an and call create Page
        this.mockMvc.perform(put("/api/admin/site/site1").content(content).contentType(MediaType.APPLICATION_JSON).principal(new UsernamePasswordAuthenticationToken("Bob", ""))).
                andExpect(status().isOk()).andExpect(content().json("[{\"name\":\"OneWiki\", \"hostname\":\"wiki.com\", \"siteName\":\"One Wiki\"},{\"name\":\"TwoWiki\"}]"));
        verify(siteService).addSite("site1", "site.com", "Site 1");
        verify(pageUpdateService, never()).createDefaultSiteHomepage(any(), any(), any());

        // Create site passes, then create page
        content = "{\"name\":\"site2\", \"siteName\":\"Site 2\", \"hostName\":\"site2.com\"}";
        when(siteService.addSite("site2", "site2.com", "Site 2")).thenReturn(true);
        this.mockMvc.perform(put("/api/admin/site/site2").content(content).contentType(MediaType.APPLICATION_JSON).principal(new UsernamePasswordAuthenticationToken("Bob", ""))).
                andExpect(status().isOk()).andExpect(content().json("[{\"name\":\"OneWiki\", \"hostname\":\"wiki.com\", \"siteName\":\"One Wiki\"},{\"name\":\"TwoWiki\"}]"));
        verify(pageUpdateService).createDefaultSiteHomepage("site2", "Site 2", "Bob");
    }

    @Test
    void delSite() throws Exception {
        User adminUser = new User();
        adminUser.roles = List.of(new UserRole(adminUser, "ROLE_ADMIN"));
        adminUser.userName = "Bob";
        when(userService.getUser("Bob")).thenReturn(adminUser);
        User regularUser = new User();
        regularUser.roles = List.of(new UserRole(adminUser, "ROLE_USER"));
        regularUser.userName = "Frank";
        when(userService.getUser("Frank")).thenReturn(regularUser);
        when(siteService.getAllSites(any())).thenReturn(List.of(new Site("OneWiki", "wiki.com", "One Wiki"), new Site("TwoWiki", "wiki2.com", "Two Wiki")));

        // No for Frank
        this.mockMvc.perform(delete("/api/admin/site/site1").principal(new UsernamePasswordAuthenticationToken("Frank", ""))).
                andExpect(status().isUnauthorized());

        // Yes forBob
        this.mockMvc.perform(delete("/api/admin/site/site1").principal(new UsernamePasswordAuthenticationToken("Bob", ""))).
                andExpect(status().isOk()).andExpect(content().json("[{\"name\":\"OneWiki\", \"hostname\":\"wiki.com\", \"siteName\":\"One Wiki\"},{\"name\":\"TwoWiki\"}]"));
        verify(siteDelService).deleteSiteCompletely("site1", "Bob");
    }

    @Test
    void setSiteSettings() throws Exception {
        User adminUser = new User();
        adminUser.roles = List.of(new UserRole(adminUser, "ROLE_ADMIN"));
        when(userService.getUser("Bob")).thenReturn(adminUser);
        User siteAdminUser = new User();
        siteAdminUser.roles = List.of(new UserRole(adminUser, "ROLE_ADMIN:TestWiki"));
        when(userService.getUser("Jake")).thenReturn(siteAdminUser);

        Site site = new Site("TestWiki", "wiki.com", "Test Wiki");
        when(siteService.setSiteSettings(eq("TestWiki"), eq("wiki.com"), eq("new settings"), any()))
                .thenReturn(site);

        this.mockMvc.perform(post("/api/admin/site/settings/TestWiki")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hostName\":\"wiki.com\", \"siteSettings\":\"new settings\"}")
                        .principal(new UsernamePasswordAuthenticationToken("Bob", "")))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"site\":{\"name\":\"TestWiki\",\"hostname\":\"wiki.com\",\"siteName\":\"Test Wiki\"},\"success\":true,\"msg\":\"\"}"));

        this.mockMvc.perform(post("/api/admin/site/settings/TestWiki")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hostName\":\"wiki.com\", \"siteSettings\":\"new settings\"}")
                        .principal(new UsernamePasswordAuthenticationToken("Jake", "")))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"site\":{\"name\":\"TestWiki\",\"hostname\":\"wiki.com\",\"siteName\":\"Test Wiki\"},\"success\":true,\"msg\":\"\"}"));


        // Test unauthorized access
        User regularUser = new User();
        regularUser.roles = List.of(new UserRole(regularUser, "ROLE_USER"));
        when(userService.getUser("Frank")).thenReturn(regularUser);

        this.mockMvc.perform(post("/api/admin/site/settings/TestWiki")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hostName\":\"wiki.com\", \"siteSettings\":\"new settings\"}")
                        .principal(new UsernamePasswordAuthenticationToken("Frank", "")))
                .andExpect(status().isUnauthorized());

        // Test exception
        when(siteService.setSiteSettings(eq("TestWiki"), eq("wiki.com"), eq("bad settings"), any()))
                .thenThrow(new SiteSettingsException("This setting is bad"));
        this.mockMvc.perform(post("/api/admin/site/settings/TestWiki")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hostName\":\"wiki.com\", \"siteSettings\":\"bad settings\"}")
                        .principal(new UsernamePasswordAuthenticationToken("Bob", "")))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"success\":false,\"msg\":\"This setting is bad\"}"));

    }
}