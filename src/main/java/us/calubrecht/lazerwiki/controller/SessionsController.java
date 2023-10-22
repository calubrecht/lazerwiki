package us.calubrecht.lazerwiki.controller;

import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("api/sessions/")
public class SessionsController {

    @GetMapping("username")
    public String username(Principal principal) {

     return principal.getName();
    }
}
