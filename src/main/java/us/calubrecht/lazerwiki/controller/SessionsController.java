package us.calubrecht.lazerwiki.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("api/sessions/")
public class SessionsController {

    @GetMapping("username")
    public String username(Principal principal) {

         return principal.getName();
    }

    @PostMapping("logout")
    public void logout(HttpSession session, Principal userP)
    {
        session.invalidate();
        SecurityContextHolder.getContext().setAuthentication(null);
        SecurityContextHolder.clearContext();
    }
}
