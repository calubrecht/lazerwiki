package us.calubrecht.lazerwiki.controller;

import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import us.calubrecht.lazerwiki.model.User;
import us.calubrecht.lazerwiki.service.RegenCacheService;
import us.calubrecht.lazerwiki.service.UserService;

import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/admin/")
public class AdminController {

    @Autowired
    RegenCacheService regenCacheService;

    @Autowired
    UserService userService;

    @PostMapping("regenLinkTable/{site}")
    public ResponseEntity<Void> regenLinkTable(@PathVariable("site") String site, Principal principal) {
        User user = userService.getUser(principal.getName());
        Set<String> roles = user.roles.stream().map(ur -> ur.role).collect(Collectors.toSet());
        if (!roles.contains("ROLE_ADMIN") && !roles.contains("ROLE_ADMIN:" + site)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        regenCacheService.regenLinks(site);
        return ResponseEntity.ok().build();
    }

    @PostMapping("regenCacheTable/{site}")
    public ResponseEntity<Void> regenCacheTable(@PathVariable("site") String site, Principal principal) {
        User user = userService.getUser(principal.getName());
        Set<String> roles = user.roles.stream().map(ur -> ur.role).collect(Collectors.toSet());
        if (!roles.contains("ROLE_ADMIN") && !roles.contains("ROLE_ADMIN:" + site)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        regenCacheService.regenCache(site);
        return ResponseEntity.ok().build();
    }

    @GetMapping("getUsers")
    public ResponseEntity<List<String>> getUsers(Principal principal) {
        User user = userService.getUser(principal.getName());
        Set<String> roles = user.roles.stream().map(ur -> ur.role).collect(Collectors.toSet());
        if (!roles.contains("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(userService.getUsers());
    }
}
