package us.calubrecht.lazerwiki.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import us.calubrecht.lazerwiki.model.User;
import us.calubrecht.lazerwiki.model.UserDTO;
import us.calubrecht.lazerwiki.model.UserRole;
import us.calubrecht.lazerwiki.service.SiteService;
import us.calubrecht.lazerwiki.service.UserService;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.Principal;

@RestController
@RequestMapping("api/sessions/")
public class SessionsController {

    @Autowired
    UserService userService;

    @Autowired
    SiteService siteService;

    @GetMapping("username")
    public UserDTO username(Principal principal, HttpServletRequest request, @CookieValue("JSESSIONID") String sessionCookie, HttpServletResponse response) throws MalformedURLException {
        URL url = new URL(request.getRequestURL().toString());
        Cookie fakeCookie = new Cookie("JESSSIONID-BACK", sessionCookie);
        fakeCookie.setHttpOnly(true);
        fakeCookie.setMaxAge(Integer.MAX_VALUE);
        fakeCookie.setPath("/");

        response.addCookie(fakeCookie);
        User user = userService.getUser(principal.getName());
        return new UserDTO(principal.getName(), siteService.getSiteForHostname(url.getHost()), user.roles.stream().map(ur -> ur.role).toList(), user.getSettings());
    }

    @PostMapping("logout")
    public void logout(HttpSession session, Principal userP)
    {
        session.setAttribute("username", null);
        session.invalidate();
        SecurityContextHolder.getContext().setAuthentication(null);
        SecurityContextHolder.clearContext();
    }
}
