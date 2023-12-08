package us.calubrecht.lazerwiki.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/csrf")
public class CsrfController {

    @GetMapping
    public void getCsrfToken(HttpServletRequest request, HttpServletResponse response) {
        // https://github.com/spring-projects/spring-security/issues/12094#issuecomment-1294150717
        request.getAttribute(CsrfToken.class.getName());
    }

}