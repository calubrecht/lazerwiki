package us.calubrecht.lazerwiki.controller;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import us.calubrecht.lazerwiki.config.JsonConfig;
import us.calubrecht.lazerwiki.model.User;
import us.calubrecht.lazerwiki.model.UserRole;
import us.calubrecht.lazerwiki.service.ExportService;
import us.calubrecht.lazerwiki.service.UserService;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static us.calubrecht.lazerwiki.controller.MvcTestUtil.unauthorized;

@WebMvcTest(controllers = {ImportExportController.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Import(JsonConfig.class)
@EnableMethodSecurity(proxyTargetClass = true)
class ImportExportControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ExportService exportService;

    @MockitoBean
    UserService userService;

    @Test
    void exportSite() throws Exception {
        User jack = new User("Jack", "");
        jack.roles = List.of(new UserRole(jack, "ROLE_ADMIN:site"));
        when(userService.getUser(eq("Jack"))).thenReturn(jack);
        Authentication auth = new UsernamePasswordAuthenticationToken("Jack", "password1");
        Mockito.doAnswer(
                inv -> {
                    OutputStream os = inv.getArgument(2, OutputStream.class);
                    os.write("This is a string".getBytes(StandardCharsets.UTF_8));
                    os.flush();
                    os.close();
                    return os;
                }).when(exportService).createExportBundle(eq("site"), eq("Jack"), any());
        this.mockMvc.perform(get("/api/io/export/site").principal(auth)).andExpect(status().isOk()).andExpect(content().string("This is a string"));
    }

    @Test
    void exportSiteNoAuth() throws Exception {
        User jack = new User("Jack", "");
        jack.roles = List.of(new UserRole(jack, "ROLE_USER"));
        when(userService.getUser(eq("Jack"))).thenReturn(jack);
        Authentication auth = new UsernamePasswordAuthenticationToken("Jack", "password1");
        unauthorized(this.mockMvc, get("/api/io/export/site").principal(auth));
        verify(exportService, never()).createExportBundle(anyString(), anyString(), any());
    }

    @Test
    void exportSiteError() throws Exception {
        User jack = new User("Jack", "");
        jack.roles = List.of(new UserRole(jack, "ROLE_ADMIN:site"));
        when(userService.getUser(eq("Jack"))).thenReturn(jack);
        Authentication auth = new UsernamePasswordAuthenticationToken("Jack", "password1");
        Mockito.doAnswer(
                inv -> {
                    throw new IOException("Could not read file");
                }).when(exportService).createExportBundle(eq("site"), eq("Jack"), any());
        // Error during streaming is initially reported as OK. But connection closes before anything sent.
        this.mockMvc.perform(get("/api/io/export/site").principal(auth)).andExpect(status().isOk()).
                andExpect(content().bytes(new byte[]{}));
    }
}