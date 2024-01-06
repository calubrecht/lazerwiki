package us.calubrecht.lazerwiki.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import us.calubrecht.lazerwiki.model.User;
import us.calubrecht.lazerwiki.model.UserDTO;
import us.calubrecht.lazerwiki.model.UserRole;
import us.calubrecht.lazerwiki.service.RegenCacheService;
import us.calubrecht.lazerwiki.service.UserService;

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
    RegenCacheService regenCacheService;

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
}