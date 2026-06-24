package us.calubrecht.lazerwiki.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import us.calubrecht.lazerwiki.config.JsonConfig;
import us.calubrecht.lazerwiki.model.User;
import us.calubrecht.lazerwiki.model.UserRole;
import us.calubrecht.lazerwiki.service.*;
import us.calubrecht.lazerwiki.service.exception.PageReadException;
import us.calubrecht.lazerwiki.service.exception.PageRevisionException;

@WebMvcTest(controllers = {PageController.class, VersionController.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Import(JsonConfig.class)
public class PageControllerTest {

  @Autowired MockMvc mockMvc;

  @MockitoBean PageService pageService;

  @MockitoBean PageSearchService pageSearchService;

  @MockitoBean PageUpdateService pageUpdateService;

  @MockitoBean RenderService renderService;

  @MockitoBean PageLockService pageLockService;

  @MockitoBean UserService userService;

  @Test
  public void test_getPage() throws Exception {
    Authentication auth = new UsernamePasswordAuthenticationToken("Bob", "password1");
    this.mockMvc.perform(get("/api/page/get/testPage").principal(auth)).andExpect(status().isOk());

    verify(renderService).getRenderedPage(eq("localhost"), eq("testPage"), eq("Bob"), any());
  }

  @Test
  public void test_getPageAnon() throws Exception {
    this.mockMvc.perform(get("/api/page/get/testPage")).andExpect(status().isOk());

    verify(renderService).getRenderedPage(eq("localhost"), eq("testPage"), eq("Guest"), any());
  }

  @Test
  public void test_getPageHistory() throws Exception {
    Authentication auth = new UsernamePasswordAuthenticationToken("Bob", "password1");
    this.mockMvc
        .perform(get("/api/page/history/testPage").principal(auth))
        .andExpect(status().isOk());

    verify(pageService).getPageHistory(eq("localhost"), eq("testPage"), eq("Bob"));

    when(pageService.getPageHistory(eq("localhost"), eq("testPage"), eq("Jack")))
        .thenThrow(new PageReadException(""));
    Authentication auth2 = new UsernamePasswordAuthenticationToken("Jack", "password1");
    this.mockMvc
        .perform(get("/api/page/history/testPage").principal(auth2))
        .andExpect(status().isForbidden());

    this.mockMvc.perform(get("/api/page/history/testPage")).andExpect(status().isOk());
    verify(pageService).getPageHistory(eq("localhost"), eq("testPage"), eq("Guest"));
  }

  @Test
  public void test_getPageDiff() throws Exception {
    Authentication auth = new UsernamePasswordAuthenticationToken("Bob", "password1");
    this.mockMvc
        .perform(get("/api/page/diff/testPage/1/2").principal(auth))
        .andExpect(status().isOk());

    verify(pageService).getPageDiff(eq("localhost"), eq("testPage"), eq(1L), eq(2L), eq("Bob"));

    when(pageService.getPageDiff(eq("localhost"), eq("testPage"), eq(1L), eq(2L), eq("Jack")))
        .thenThrow(new PageReadException(""));
    Authentication auth2 = new UsernamePasswordAuthenticationToken("Jack", "password1");
    this.mockMvc
        .perform(get("/api/page/diff/testPage/1/2").principal(auth2))
        .andExpect(status().isForbidden());

    this.mockMvc.perform(get("/api/page/diff/testPage/1/2")).andExpect(status().isOk());
    verify(pageService).getPageDiff(eq("localhost"), eq("testPage"), eq(1L), eq(2L), eq("Guest"));
  }

  @Test
  public void test_savePage() throws Exception {
    Authentication auth = new UsernamePasswordAuthenticationToken("Bob", "password1");
    String data =
        "{\"pageName\": \"thisPage\", \"text\": \"This is some text\", \"revision\": 10, \"force\": false}";
    this.mockMvc
        .perform(
            post("/api/page/testPage/savePage")
                .content(data)
                .contentType(MediaType.APPLICATION_JSON)
                .principal(auth))
        .andExpect(status().isOk());

    verify(renderService)
        .savePage(
            eq("localhost"),
            eq("testPage"),
            eq("This is some text"),
            isNull(),
            eq(10L),
            eq(false),
            eq("Bob"));

    data =
        "{\"pageName\": \"thisPage\", \"text\": \"This is some text\", \"revision\": 10, \"force\": true}";
    this.mockMvc
        .perform(
            post("/api/page/testPage/savePage")
                .content(data)
                .contentType(MediaType.APPLICATION_JSON)
                .principal(auth))
        .andExpect(status().isOk());

    verify(renderService)
        .savePage(
            eq("localhost"),
            eq("testPage"),
            eq("This is some text"),
            isNull(),
            eq(10L),
            eq(true),
            eq("Bob"));

    Mockito.doThrow(new PageRevisionException("Bad"))
        .when(renderService)
        .savePage(
            eq("localhost"),
            eq("errorPage"),
            eq("This is some test"),
            isNull(),
            eq(12L),
            eq(false),
            eq("Bob"));

    data =
        "{\"pageName\": \"errorPage\", \"text\": \"This is some test\", \"revision\": 12, \"force\": false}";
    this.mockMvc
        .perform(
            post("/api/page/errorPage/savePage")
                .content(data)
                .contentType(MediaType.APPLICATION_JSON)
                .principal(auth))
        .andExpect(status().isOk())
        .andExpect(content().json("{\"success\":false, \"msg\":\"Bad\"}"));

    this.mockMvc
            .perform(
                    post("/api/page/testPage/savePage")
                            .content(data)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    // TODO: verify guest save is logged.
  }

  @Test
  public void test_listPage() throws Exception {
    Authentication auth = new UsernamePasswordAuthenticationToken("Bob", "password1");
    this.mockMvc.perform(get("/api/page/listPages").principal(auth)).andExpect(status().isOk());

    verify(pageService).getAllPages(eq("localhost"), eq("Bob"));

    this.mockMvc.perform(get("/api/page/listPages")).andExpect(status().isOk());

    verify(pageService).getAllPages(eq("localhost"), eq("Guest"));
  }

  @Autowired ObjectMapper jsonMapper;

  @Test
  public void test_saveNewPage() throws Exception {
    Authentication auth = new UsernamePasswordAuthenticationToken("Bob", "password1");
    String data =
        "{\"pageName\": \"thisPage\", \"text\": \"This is some text\", \"revision\":null, \"force\": false}";
    this.mockMvc
        .perform(
            post("/api/page/testPage/savePage")
                .content(data)
                .contentType(MediaType.APPLICATION_JSON)
                .principal(auth))
        .andExpect(status().isOk());

    verify(renderService)
        .savePage(
            eq("localhost"),
            eq("testPage"),
            eq("This is some text"),
            isNull(),
            eq(0L),
            eq(false),
            eq("Bob"));
  }

  @Test
  public void test_listNamespaces() throws Exception {
    User user = new User("Bob", "");
    user.roles = List.of();
    when(userService.getUser("Bob")).thenReturn(user);
    User guestUser = new User(User.GUEST, "");
    guestUser.roles = List.of();
    when(userService.getUser(User.GUEST)).thenReturn(guestUser);

    Authentication bobAuth = new UsernamePasswordAuthenticationToken("Bob", "password");

    // Non-admin and guest are forbidden
    this.mockMvc
        .perform(get("/api/page/listNamespaces/site1").principal(bobAuth))
        .andExpect(status().isForbidden());
    this.mockMvc.perform(get("/api/page/listNamespaces/site1")).andExpect(status().isForbidden());

    // Global admin can access any site
    User adminUser = new User("Admin", "");
    adminUser.roles = List.of(new UserRole(adminUser, "ROLE_ADMIN"));
    when(userService.getUser("Admin")).thenReturn(adminUser);
    Authentication adminAuth = new UsernamePasswordAuthenticationToken("Admin", "password1");
    this.mockMvc
        .perform(get("/api/page/listNamespaces/site1").principal(adminAuth))
        .andExpect(status().isOk());
    verify(pageService).getAllNamespaces(eq("site1"), eq("Admin"));

    // Site admin can access their own site
    User siteAdminUser = new User("SiteAdmin", "");
    siteAdminUser.roles = List.of(new UserRole(siteAdminUser, "ROLE_ADMIN:site1"));
    when(userService.getUser("SiteAdmin")).thenReturn(siteAdminUser);
    Authentication siteAdminAuth =
        new UsernamePasswordAuthenticationToken("SiteAdmin", "password1");
    this.mockMvc
        .perform(get("/api/page/listNamespaces/site1").principal(siteAdminAuth))
        .andExpect(status().isOk());
    verify(pageService).getAllNamespaces(eq("site1"), eq("SiteAdmin"));
  }

  @Test
  public void test_listTags() throws Exception {
    Authentication auth = new UsernamePasswordAuthenticationToken("Bob", "password1");
    when(pageService.getAllTags(any(), any())).thenReturn(List.of("tag1", "tag2"));
    String data = "[\"tag1\", \"tag2\"]";
    this.mockMvc
        .perform(get("/api/page/listTags").principal(auth))
        .andExpect(status().isOk())
        .andExpect(content().json(data));

    verify(pageService).getAllTags(eq("localhost"), eq("Bob"));

    this.mockMvc
        .perform(get("/api/page/listTags"))
        .andExpect(status().isOk())
        .andExpect(content().json(data));

    verify(pageService).getAllTags(eq("localhost"), eq("Guest"));
  }

  @Test
  public void test_searchPages() throws Exception {
    Authentication auth = new UsernamePasswordAuthenticationToken("Bob", "password1");
    this.mockMvc
        .perform(get("/api/page/searchPages?search=tag:common").principal(auth))
        .andExpect(status().isOk());

    verify(pageSearchService).searchPages(eq("localhost"), eq("Bob"), eq("tag:common"));

    this.mockMvc.perform(get("/api/page/searchPages?search=tag:common")).andExpect(status().isOk());

    verify(pageSearchService).searchPages(eq("localhost"), eq("Guest"), eq("tag:common"));
  }

  @Test
  public void test_deletePage() throws Exception {
    Authentication auth = new UsernamePasswordAuthenticationToken("Bob", "password1");
    this.mockMvc.perform(delete("/api/page/testPage").principal(auth)).andExpect(status().isOk());

    verify(pageUpdateService).deletePage(eq("localhost"), eq("testPage"), eq("Bob"));
  }

  @Test
  public void test_previewPage() throws Exception {
    Authentication auth = new UsernamePasswordAuthenticationToken("Bob", "password1");
    String data = "{\"pageName\": \"thisPage\", \"text\": \"This is some text\"}";
    this.mockMvc
        .perform(
            post("/api/page/previewPage/thisPage")
                .content(data)
                .contentType(MediaType.APPLICATION_JSON)
                .principal(auth))
        .andExpect(status().isOk());

    verify(renderService)
        .previewPage(eq("localhost"), eq("thisPage"), eq("This is some text"), eq("Bob"), any());
    this.mockMvc
        .perform(
            post("/api/page/previewPage/")
                .content(data)
                .contentType(MediaType.APPLICATION_JSON)
                .principal(auth))
        .andExpect(status().isOk());

    verify(renderService)
        .previewPage(eq("localhost"), eq(""), eq("This is some text"), eq("Bob"), any());
  }

  @Test
  public void test_getPageHistorical() throws Exception {
    Authentication auth = new UsernamePasswordAuthenticationToken("Bob", "password1");
    this.mockMvc
        .perform(get("/api/page/getHistorical/testPage/1").principal(auth))
        .andExpect(status().isOk());

    verify(renderService)
        .getHistoricalRenderedPage(eq("localhost"), eq("testPage"), eq(1L), eq("Bob"));

    this.mockMvc.perform(get("/api/page/getHistorical/testPage/1")).andExpect(status().isOk());

    verify(renderService)
        .getHistoricalRenderedPage(eq("localhost"), eq("testPage"), eq(1L), eq("Guest"));
  }

  @Test
  public void test_getRecentChanges() throws Exception {
    Authentication auth = new UsernamePasswordAuthenticationToken("Bob", "password1");
    this.mockMvc.perform(get("/api/page/recentChanges").principal(auth)).andExpect(status().isOk());

    verify(pageService).recentChanges(eq("localhost"), eq("Bob"));

    this.mockMvc.perform(get("/api/page/recentChanges")).andExpect(status().isOk());
    verify(pageService).recentChanges(eq("localhost"), eq("Guest"));
  }

  @Test
  public void test_getPageLock() throws Exception {
    Authentication auth = new UsernamePasswordAuthenticationToken("Bob", "password1");
    this.mockMvc
        .perform(
            post("/api/page/lock/testPage").contentType(MediaType.APPLICATION_JSON).principal(auth))
        .andExpect(status().isOk());

    verify(pageLockService).getPageLock(eq("localhost"), eq("testPage"), eq("Bob"), eq(false));

    this.mockMvc
        .perform(
            post("/api/page/lock/testPage?overrideLock=true")
                .contentType(MediaType.APPLICATION_JSON)
                .principal(auth))
        .andExpect(status().isOk());

    verify(pageLockService).getPageLock(eq("localhost"), eq("testPage"), eq("Bob"), eq(true));
  }

  @Test
  public void test_releasePageLock() throws Exception {
    Authentication auth = new UsernamePasswordAuthenticationToken("Bob", "password1");
    this.mockMvc
        .perform(
            post("/api/page/releaseLock/testPage/id/abcd")
                .contentType(MediaType.APPLICATION_JSON)
                .principal(auth))
        .andExpect(status().isOk());

    verify(pageLockService).releasePageLock(eq("localhost"), eq("testPage"), eq("abcd"), eq("Bob"));

    this.mockMvc
        .perform(
            post("/api/page/releaseLock/id/abcd")
                .contentType(MediaType.APPLICATION_JSON)
                .principal(auth))
        .andExpect(status().isOk());

    verify(pageLockService).releasePageLock(eq("localhost"), eq(""), eq("abcd"), eq("Bob"));
  }

  @Test
  public void test_movePage() throws Exception {
    Authentication auth = new UsernamePasswordAuthenticationToken("Bob", "password1");
    String data =
        "{\"oldNS\": \"ns1\", \"oldPage\": \"abcPage\", \"newNS\": \"ns2\", \"newPage\": \"defPage\"}";
    this.mockMvc
        .perform(
            post("/api/page/abcPage/movePage")
                .content(data)
                .contentType(MediaType.APPLICATION_JSON)
                .principal(auth))
        .andExpect(status().isOk());

    verify(pageUpdateService)
        .movePage(eq("localhost"), eq("Bob"), eq("ns1"), eq("abcPage"), eq("ns2"), eq("defPage"));
  }
}
