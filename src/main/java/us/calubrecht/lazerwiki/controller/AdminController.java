package us.calubrecht.lazerwiki.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import us.calubrecht.lazerwiki.model.User;
import us.calubrecht.lazerwiki.service.AdminService;
import us.calubrecht.lazerwiki.service.UserService;

import java.security.Principal;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/admin/")
public class AdminController {

    @Autowired
    AdminService adminService;

    @Autowired
    UserService userService;

    @PostMapping("regenLinkTable/{site}")
    public ResponseEntity<Void> regenLinkTable(@PathVariable("site") String site, Principal principal) {
        User user = userService.getUser(principal.getName());
        Set<String> roles = user.roles.stream().map(ur -> ur.role).collect(Collectors.toSet());
        if (!roles.contains("ROLE_ADMIN") && !roles.contains("ROLE_ADMIN:" + site)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        adminService.regenLinks(site);
        return ResponseEntity.ok().build();
    }

    @PostMapping("regenCacheTable/{site}")
    public ResponseEntity<Void> regenCacheTable(@PathVariable("site") String site, Principal principal) {
        User user = userService.getUser(principal.getName());
        Set<String> roles = user.roles.stream().map(ur -> ur.role).collect(Collectors.toSet());
        if (!roles.contains("ROLE_ADMIN") && !roles.contains("ROLE_ADMIN:" + site)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        adminService.regenCache(site);
        return ResponseEntity.ok().build();
    }
}
