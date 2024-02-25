package us.calubrecht.lazerwiki.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import us.calubrecht.lazerwiki.LazerWikiAuthenticationManager;
import us.calubrecht.lazerwiki.model.Site;
import us.calubrecht.lazerwiki.model.User;
import us.calubrecht.lazerwiki.model.UserDTO;
import us.calubrecht.lazerwiki.model.UserRequest;
import us.calubrecht.lazerwiki.requests.SiteRequest;
import us.calubrecht.lazerwiki.service.PageUpdateService;
import us.calubrecht.lazerwiki.service.RegenCacheService;
import us.calubrecht.lazerwiki.service.SiteService;
import us.calubrecht.lazerwiki.service.UserService;
import us.calubrecht.lazerwiki.service.exception.PageWriteException;

import java.io.IOException;
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

    @Autowired
    SiteService siteService;

    @Autowired
    PageUpdateService pageUpdateService;

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
    public ResponseEntity<List<UserDTO>> getUsers(Principal principal) {
        User user = userService.getUser(principal.getName());
        Set<String> roles = user.roles.stream().map(ur -> ur.role).collect(Collectors.toSet());
        if (!roles.contains("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(userService.getUsers());
    }

    @DeleteMapping("role/{userName}/{userRole}")
    public ResponseEntity<UserDTO> deleteRole(Principal principal, @PathVariable("userName") String userName, @PathVariable("userRole") String userRole) {
        User user = userService.getUser(principal.getName());
        Set<String> roles = user.roles.stream().map(ur -> ur.role).collect(Collectors.toSet());
        if (!roles.contains("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (user.userName.equals(userName) && userRole.equals("ROLE_ADMIN")) {
            // Cannot remove your own admin role
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(userService.deleteRole(userName, userRole));
    }

    @PutMapping("role/{userName}/{userRole}")
    public ResponseEntity<UserDTO> addRole(Principal principal, @PathVariable("userName") String userName, @PathVariable("userRole") String userRole) {
        User user = userService.getUser(principal.getName());
        Set<String> roles = user.roles.stream().map(ur -> ur.role).collect(Collectors.toSet());
        if (!roles.contains("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(userService.addRole(userName, userRole));
    }

    @PutMapping("user/{userName}")
    public ResponseEntity<UserDTO> addUser(Principal principal, @PathVariable("userName") String userName, @RequestBody UserRequest userRequest) {
        User user = userService.getUser(principal.getName());
        Set<String> roles = user.roles.stream().map(ur -> ur.role).collect(Collectors.toSet());
        if (!roles.contains("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (userService.getUser(userName) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        userService.addUser(userName, userRequest.password(), List.of(LazerWikiAuthenticationManager.USER));
        User u = userService.getUser(userName);
        UserDTO dto = new UserDTO(u.userName, null, u.roles.stream().map(role -> role.role).toList());
        return ResponseEntity.ok(dto);
    }

    @PostMapping("passwordReset/{userName}")
    public ResponseEntity<Void> resetPassword(Principal principal, @PathVariable("userName") String userName, @RequestBody UserRequest userRequest) {
        User user = userService.getUser(principal.getName());
        Set<String> roles = user.roles.stream().map(ur -> ur.role).collect(Collectors.toSet());
        if (!roles.contains("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        userService.resetPassword(userName, userRequest.password());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("user/{userName}")
    public ResponseEntity<Void> deleteUser(Principal principal, @PathVariable("userName") String userName) {
        User user = userService.getUser(principal.getName());
        Set<String> roles = user.roles.stream().map(ur -> ur.role).collect(Collectors.toSet());
        if (!roles.contains("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        userService.deleteUser(userName);
        return ResponseEntity.ok().build();
    }

    @GetMapping("sites")
    public List<Site> getAllSites(Principal principal) {
        User user = userService.getUser(principal.getName());
        return siteService.getAllSites(user);
    }

    @PutMapping("site/{siteName}")
    public ResponseEntity<List<Site>> addSite(Principal principal, @PathVariable("siteName") String siteName, @RequestBody SiteRequest siteRequest) throws PageWriteException, IOException {
        User user = userService.getUser(principal.getName());
        Set<String> roles = user.roles.stream().map(ur -> ur.role).collect(Collectors.toSet());
        if (!roles.contains("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (siteService.addSite(siteRequest.name(), siteRequest.hostName(), siteRequest.siteName() )) {
            pageUpdateService.createDefaultSiteHomepage(siteRequest.name(), siteRequest.siteName(), user.userName);
        }
        return ResponseEntity.ok(siteService.getAllSites(user));
    }


}
