package us.calubrecht.lazerwiki.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import us.calubrecht.lazerwiki.model.UserDTO;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.Principal;

@RestController
@RequestMapping("api/sessions/")
public class SessionsController {

    @GetMapping("username")
    public UserDTO username(Principal principal, HttpServletRequest request) throws MalformedURLException {
        URL url = new URL(request.getRequestURL().toString());
        return new UserDTO(principal.getName(), url.getHost(), null);
    }

    @PostMapping("logout")
    public void logout(HttpSession session, Principal userP)
    {
        session.invalidate();
        SecurityContextHolder.getContext().setAuthentication(null);
        SecurityContextHolder.clearContext();
    }
}
