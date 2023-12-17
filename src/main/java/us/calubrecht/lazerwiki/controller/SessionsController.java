package us.calubrecht.lazerwiki.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
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
    public UserDTO username(Principal principal, HttpServletRequest request) throws MalformedURLException {
        URL url = new URL(request.getRequestURL().toString());
        return new UserDTO(principal.getName(), siteService.getSiteForHostname(url.getHost()), userService.getUser(principal.getName()).roles.stream().map(ur -> ur.role).toList());
    }

    @PostMapping("logout")
    public void logout(HttpSession session, Principal userP)
    {
        session.invalidate();
        SecurityContextHolder.getContext().setAuthentication(null);
        SecurityContextHolder.clearContext();
    }
}
