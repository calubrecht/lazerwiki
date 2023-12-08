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
import us.calubrecht.lazerwiki.model.UserRole;
import us.calubrecht.lazerwiki.service.RegenCacheService;
import us.calubrecht.lazerwiki.service.UserService;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
}