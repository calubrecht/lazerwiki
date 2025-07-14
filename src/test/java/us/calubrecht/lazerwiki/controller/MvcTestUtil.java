package us.calubrecht.lazerwiki.controller;

import jakarta.servlet.ServletException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MvcTestUtil {

    public static void unauthorized(MockMvc mvc, RequestBuilder request) {
        ServletException exc = assertThrows(ServletException.class, () -> mvc.perform(request));
        assertEquals(AuthorizationDeniedException.class, exc.getCause().getClass());
    }
}
