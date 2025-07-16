package us.calubrecht.lazerwiki.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import us.calubrecht.lazerwiki.model.*;
import us.calubrecht.lazerwiki.responses.NsNode;
import us.calubrecht.lazerwiki.responses.PageListResponse;
import us.calubrecht.lazerwiki.service.*;
import us.calubrecht.lazerwiki.service.exception.SiteSettingsException;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static us.calubrecht.lazerwiki.controller.MvcTestUtil.unauthorized;

@WebMvcTest(controllers = {AdminController.class, VersionController.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@EnableMethodSecurity(proxyTargetClass = true)
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

    @MockBean
    NamespaceService namespaceService;

    @MockBean
    PageService pageService;

    @MockBean
    GlobalSettingsService globalSettingsService;

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
        unauthorized(mockMvc, post("/api/admin/regenLinkTable/default").principal(new UsernamePasswordAuthenticationToken("frank", "")));

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
        unauthorized(mockMvc, post("/api/admin/regenCacheTable/default").principal(new UsernamePasswordAuthenticationToken("frank", "")));
        verify(regenCacheService, times(2)).regenCache("default");
    }

    @Test
    void getUsers() throws Exception {
        when(userService.getUsers()).thenReturn(List.of(new UserDTO("Bob",null, List.of("ROLE_ADMIN","ROLE_USER"), Map.of()), new UserDTO("Frank", null, List.of("ROLE_USER"), Map.of())));
        User adminUser = new User();
        adminUser.roles = List.of(new UserRole(adminUser, "ROLE_ADMIN"));
        when(userService.getUser("bob")).thenReturn(adminUser);
        User regularUser = new User();
        regularUser.roles = List.of(new UserRole(regularUser, "ROLE_USER"));
        when(userService.getUser("frank")).thenReturn(regularUser);
        User userAdminUser = new User();
        userAdminUser.roles = List.of(new UserRole(adminUser, "ROLE_USERADMIN"));
        userAdminUser.userName = "Joe";
        when(userService.getUser("Joe")).thenReturn(userAdminUser);

        this.mockMvc.perform(get("/api/admin/getUsers").principal(new UsernamePasswordAuthenticationToken("bob", ""))).
        andExpect(status().isOk()).andExpect(content().json("[{\"userName\":\"Bob\", \"userRoles\":[\"ROLE_ADMIN\", \"ROLE_USER\"]}, {\"userName\":\"Frank\", \"userRoles\":[\"ROLE_USER\"]}]"));

        this.mockMvc.perform(get("/api/admin/getUsers").principal(new UsernamePasswordAuthenticationToken("Joe", ""))).
                andExpect(status().isOk()).andExpect(content().json("[{\"userName\":\"Bob\", \"userRoles\":[\"ROLE_ADMIN\", \"ROLE_USER\"]}, {\"userName\":\"Frank\", \"userRoles\":[\"ROLE_USER\"]}]"));

        unauthorized(mockMvc, get("/api/admin/getUsers").principal(new UsernamePasswordAuthenticationToken("frank", "")));
    }

    @Test
    void deleteRole() throws Exception {
        when(userService.deleteRole(eq("Frank"), eq("ROLE_ADMIN"), any())).thenReturn(new UserDTO("Frank",null, List.of("ROLE_USER"), Map.of()));
        when(userService.deleteRole(eq("Bob"), eq("ROLE_EXTRA"), any())).thenReturn(new UserDTO("Bob",null, List.of("ROLE_ADMIN"), Map.of()));
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
        unauthorized(this.mockMvc, delete("/api/admin/role/Bob/ROLE_ADMIN").principal(new UsernamePasswordAuthenticationToken("Bob", "")));

        // But can delete other own roles
        this.mockMvc.perform(delete("/api/admin/role/Bob/ROLE_EXTRA").principal(new UsernamePasswordAuthenticationToken("Bob", ""))).
                andExpect(status().isOk()).andExpect(content().json("{\"userName\":\"Bob\", \"userRoles\":[\"ROLE_ADMIN\"]}"));

        // Only Admin can delete
        unauthorized(this.mockMvc, delete("/api/admin/role/Frank/ROLE_EXTRA").principal(new UsernamePasswordAuthenticationToken("Frank", "")));
    }

    @Test
    void addRoles() throws Exception {
        when(userService.addRole(eq("Frank"), eq("ROLE_ADMIN"), any())).thenReturn(new UserDTO("Frank",null, List.of("ROLE_USER"), Map.of()));
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
        unauthorized(this.mockMvc, put("/api/admin/role/Frank/ROLE_EXTRA").principal(new UsernamePasswordAuthenticationToken("Frank", "")));
    }

    @Test
    void setRoles() throws Exception {
        when(userService.setSiteRoles(eq("Frank"), eq("site1"), anyList(), any())).thenReturn(new UserDTO("Frank",null, List.of("ROLE_USER"), Map.of()));
        User adminUser = new User();
        adminUser.roles = List.of(new UserRole(adminUser, "ROLE_ADMIN"));
        adminUser.userName = "Bob";
        when(userService.getUser("Bob")).thenReturn(adminUser);
        User regularUser = new User();
        regularUser.roles = List.of(new UserRole(adminUser, "ROLE_USER"));
        regularUser.userName = "Frank";
        when(userService.getUser("Frank")).thenReturn(regularUser);
        User siteAdmin = new User();
        siteAdmin.roles = List.of(new UserRole(adminUser, "ROLE_ADMIN:site1"));
        siteAdmin.userName = "Joey";
        when(userService.getUser("Joey")).thenReturn(siteAdmin);
        this.mockMvc.perform(put("/api/admin/roles/Frank/site/site1").principal(new UsernamePasswordAuthenticationToken("Bob", "")).content(
                "[\"ROLE_READ:site1:bo\"]"
                ).contentType(MediaType.APPLICATION_JSON)).
                andExpect(status().isOk()).andExpect(content().json("{\"userName\":\"Frank\", \"userRoles\":[\"ROLE_USER\"]}"));
        verify(userService).setSiteRoles(eq("Frank"), eq("site1"), eq(List.of("ROLE_READ:site1:bo")), any());

        // Only Admin can add role
        unauthorized(this.mockMvc,put("/api/admin/roles/Frank/site/site1").principal(new UsernamePasswordAuthenticationToken("Frank", "")).content(
                "[\"ROLE_READ:site1:bo\"]"
                ).contentType(MediaType.APPLICATION_JSON));

        // Site admin can add for site, but not other site
        this.mockMvc.perform(put("/api/admin/roles/Frank/site/site1").principal(new UsernamePasswordAuthenticationToken("Joey", "")).content(
                        "[\"ROLE_READ:site1:bo\"]"
                ).contentType(MediaType.APPLICATION_JSON)).
                andExpect(status().isOk()).andExpect(content().json("{\"userName\":\"Frank\", \"userRoles\":[\"ROLE_USER\"]}"));
        verify(userService, times(2)).setSiteRoles(eq("Frank"), eq("site1"), eq(List.of("ROLE_READ:site1:bo")), any());

        unauthorized(this.mockMvc,put("/api/admin/roles/Frank/site/site2").principal(new UsernamePasswordAuthenticationToken("Joey", "")).content(
                        "[\"ROLE_READ:site2:bo\"]"
                ).contentType(MediaType.APPLICATION_JSON));

        // And flag invalid roles
        unauthorized(this.mockMvc, put("/api/admin/roles/Frank/site/site1").principal(new UsernamePasswordAuthenticationToken("Joey", "")).content(
                        "[\"ROLE_READ:site2:bo\"]"
                ).contentType(MediaType.APPLICATION_JSON));

        unauthorized(this.mockMvc, put("/api/admin/roles/Frank/site/site1").principal(new UsernamePasswordAuthenticationToken("Joey", "")).content(
                        "[\"ROLE_ADMIN\"]"
                ).contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void addUser() throws Exception {
        User u =  new User("NewUser", null);
        u.roles = List.of(new UserRole(u, "ROLE_USER"));
        when(userService.getUser(eq("NewUser"))).thenReturn(null, u, null, u);
        User adminUser = new User();
        adminUser.roles = List.of(new UserRole(adminUser, "ROLE_ADMIN"));
        adminUser.userName = "Bob";
        when(userService.getUser("Bob")).thenReturn(adminUser);
        User userAdminUser = new User();
        userAdminUser.roles = List.of(new UserRole(adminUser, "ROLE_USERADMIN"));
        userAdminUser.userName = "Joe";
        when(userService.getUser("Joe")).thenReturn(userAdminUser);
        User regularUser = new User();
        regularUser.roles = List.of(new UserRole(adminUser, "ROLE_USER"));
        regularUser.userName = "Frank";
        when(userService.getUser("Frank")).thenReturn(regularUser);
        GlobalSettings settings = new GlobalSettings();
        settings.settings = Map.of(GlobalSettings.ENABLE_SELF_REG, false);
        when(globalSettingsService.getSettings()).thenReturn(settings);
        this.mockMvc.perform(put("/api/admin/user/NewUser").content("{\"userName\": \"NewUser\", \"password\": \"password\"}").contentType(MediaType.APPLICATION_JSON).principal(new UsernamePasswordAuthenticationToken("Bob", ""))).
                andExpect(status().isOk()).andExpect(content().json("{\"userName\":\"NewUser\", \"userRoles\":[\"ROLE_USER\"]}"));
        this.mockMvc.perform(put("/api/admin/user/NewUser").content("{\"userName\": \"NewUser\", \"password\": \"password\"}").contentType(MediaType.APPLICATION_JSON).principal(new UsernamePasswordAuthenticationToken("Joe", ""))).
                andExpect(status().isOk()).andExpect(content().json("{\"userName\":\"NewUser\", \"userRoles\":[\"ROLE_USER\"]}"));

        // Only Admin can add user
        unauthorized(this.mockMvc, put("/api/admin/user/NewUser").content("{\"userName\": \"NewUser\", \"password\": \"password\"}").contentType(MediaType.APPLICATION_JSON).principal(new UsernamePasswordAuthenticationToken("Frank", "")));

        // Cannot add user that already exists
        this.mockMvc.perform(put("/api/admin/user/NewUser").content("{\"userName\": \"NewUser\", \"password\": \"password\"}").contentType(MediaType.APPLICATION_JSON).principal(new UsernamePasswordAuthenticationToken("Bob", ""))).
                andExpect(status().isConflict());

        // If enableSelfReg is on, can self register
        settings.settings = Map.of(GlobalSettings.ENABLE_SELF_REG, true);
        when(userService.getUser(eq("NewerUser"))).thenReturn(null, u);
        when(userService.getUser(eq("NewestUser"))).thenReturn(null, u);
        this.mockMvc.perform(put("/api/admin/user/NewerUser").content("{\"userName\": \"NewerUser\", \"password\": \"password\"}").contentType(MediaType.APPLICATION_JSON)).
                andExpect(status().isOk());

        // if not, cannot
        settings.settings = Map.of(GlobalSettings.ENABLE_SELF_REG, false);
        unauthorized(this.mockMvc, put("/api/admin/user/NewestUser").content("{\"userName\": \"NewestUser\", \"password\": \"password\"}").contentType(MediaType.APPLICATION_JSON));
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
        User userAdminUser = new User();
        userAdminUser.roles = List.of(new UserRole(adminUser, "ROLE_USERADMIN"));
        userAdminUser.userName = "Joe";
        when(userService.getUser("Joe")).thenReturn(userAdminUser);
        User regularUser = new User();
        regularUser.roles = List.of(new UserRole(adminUser, "ROLE_USER"));
        regularUser.userName = "Frank";
        when(userService.getUser("Frank")).thenReturn(regularUser);
        this.mockMvc.perform(post("/api/admin/passwordReset/User").content("{\"userName\": \"User\", \"password\": \"password\"}").contentType(MediaType.APPLICATION_JSON).principal(new UsernamePasswordAuthenticationToken("Bob", ""))).
                andExpect(status().isOk());

        verify(userService).resetPassword("User", "password");

        this.mockMvc.perform(post("/api/admin/passwordReset/User").content("{\"userName\": \"User\", \"password\": \"password\"}").contentType(MediaType.APPLICATION_JSON).principal(new UsernamePasswordAuthenticationToken("Joe", ""))).
                andExpect(status().isOk());

        verify(userService, times(2)).resetPassword("User", "password");
        // Only Admin can add user
        unauthorized(this.mockMvc, post("/api/admin/passwordReset/User").content("{\"userName\": \"User\", \"password\": \"password\"}").contentType(MediaType.APPLICATION_JSON).principal(new UsernamePasswordAuthenticationToken("Frank", "")));
        verify(userService, times(2)).resetPassword("User", "password");
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

        verify(userService).deleteUser("User", adminUser);
        // Only Admin can delete user
        unauthorized(this.mockMvc, delete("/api/admin/user/User").principal(new UsernamePasswordAuthenticationToken("Frank", "")));
        verify(userService, times(1)).deleteUser(eq("User"), any());
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
        unauthorized(this.mockMvc, put("/api/admin/site/site1").content(content).contentType(MediaType.APPLICATION_JSON).principal(new UsernamePasswordAuthenticationToken("Frank", "")));

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
        unauthorized(this.mockMvc, delete("/api/admin/site/site1").principal(new UsernamePasswordAuthenticationToken("Frank", "")));

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

        unauthorized(this.mockMvc, post("/api/admin/site/settings/TestWiki")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hostName\":\"wiki.com\", \"siteSettings\":\"new settings\"}")
                        .principal(new UsernamePasswordAuthenticationToken("Frank", "")));

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

    @Test
    void setNamespaceRestrictionType() throws Exception {
        User adminUser = new User();
        adminUser.roles = List.of(new UserRole(adminUser, "ROLE_ADMIN"));
        when(userService.getUser("Bob")).thenReturn(adminUser);
        User siteAdminUser = new User();
        siteAdminUser.roles = List.of(new UserRole(adminUser, "ROLE_ADMIN:site1"));
        when(userService.getUser("Jake")).thenReturn(siteAdminUser);

        NsNode namespaces = new NsNode("", true);
        PageListResponse response = new PageListResponse(null, namespaces);
        when(pageService.getAllNamespaces(eq("site1"), any()))
                .thenReturn(response);

        this.mockMvc.perform(post("/api/admin/namespace/restrictionType")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"site\":\"site1\", \"namespace\":\"ns\", \"restrictionType\": \"OPEN\"}")
                        .principal(new UsernamePasswordAuthenticationToken("Bob", "")))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"namespaces\":{\"children\":[],\"namespace\":\"\",\"fullNamespace\":\"\"}}"));

        verify(namespaceService).setNSRestriction("site1", "ns", Namespace.RESTRICTION_TYPE.OPEN);

        this.mockMvc.perform(post("/api/admin/namespace/restrictionType")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"site\":\"site1\", \"namespace\":\"ns\", \"restrictionType\": \"OPEN\"}")
                        .principal(new UsernamePasswordAuthenticationToken("Jake", "")))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"namespaces\":{\"children\":[],\"namespace\":\"\",\"fullNamespace\":\"\"}}"));


        // Test unauthorized access
        User regularUser = new User();
        regularUser.roles = List.of(new UserRole(regularUser, "ROLE_USER"));
        when(userService.getUser("Frank")).thenReturn(regularUser);

        unauthorized(this.mockMvc, post("/api/admin/namespace/restrictionType")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"site\":\"site1\", \"namespace\":\"ns\", \"restrictionType\": \"OPEN\"}")
                        .principal(new UsernamePasswordAuthenticationToken("Frank", "")));
    }

    @Test
    void getGlobalSettings() throws Exception {
        User adminUser = new User();
        adminUser.roles = List.of(new UserRole(adminUser, "ROLE_ADMIN"));
        when(userService.getUser("Bob")).thenReturn(adminUser);

        GlobalSettings settings = new GlobalSettings();
        settings.settings = Map.of("Setting1", "value1");
        when(globalSettingsService.getSettings()).thenReturn(settings);

        this.mockMvc.perform(get("/api/admin/globalSettings")
                        .principal(new UsernamePasswordAuthenticationToken("Bob", "")))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"settings\":{\"Setting1\":\"value1\"}}"));
    }

    @Test
    void setGlobalSettings() throws Exception {
        User adminUser = new User();
        adminUser.roles = List.of(new UserRole(adminUser, "ROLE_ADMIN"));
        when(userService.getUser("Bob")).thenReturn(adminUser);
        User siteAdminUser = new User();
        siteAdminUser.roles = List.of(new UserRole(adminUser, "ROLE_ADMIN:TestWiki"));
        when(userService.getUser("Jake")).thenReturn(siteAdminUser);
        GlobalSettings settings = new GlobalSettings();
        settings.settings = Map.of("Setting1", "value1");

        this.mockMvc.perform(post("/api/admin/globalSettings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"settings\": {\"Setting1\":\"value1\"}}")
                        .principal(new UsernamePasswordAuthenticationToken("Bob", "")))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"success\": true}"));

        verify(globalSettingsService).setSettings(settings);

        unauthorized(this.mockMvc, post("/api/admin/globalSettings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"settings\": {\"Setting1\":\"value1\"}}")
                        .principal(new UsernamePasswordAuthenticationToken("Jake", "")));
    }
}