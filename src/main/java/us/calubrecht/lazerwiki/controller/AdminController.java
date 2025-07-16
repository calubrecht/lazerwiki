package us.calubrecht.lazerwiki.controller;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.*;
import us.calubrecht.lazerwiki.LazerWikiAuthenticationManager;
import us.calubrecht.lazerwiki.model.*;
import us.calubrecht.lazerwiki.requests.NamespaceRestrictionRequest;
import us.calubrecht.lazerwiki.requests.SiteRequest;
import us.calubrecht.lazerwiki.requests.SiteSettingsRequest;
import us.calubrecht.lazerwiki.responses.CommonResponse;
import us.calubrecht.lazerwiki.responses.PageListResponse;
import us.calubrecht.lazerwiki.responses.SiteSettingsResponse;
import us.calubrecht.lazerwiki.service.*;
import us.calubrecht.lazerwiki.service.exception.MediaWriteException;
import us.calubrecht.lazerwiki.service.exception.PageWriteException;
import us.calubrecht.lazerwiki.service.exception.SiteSettingsException;

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
    SiteDelService siteDelService;

    @Autowired
    PageUpdateService pageUpdateService;

    @Autowired
    NamespaceService namespaceService;

    @Autowired
    PageService pageService;

    @Autowired
    GlobalSettingsService globalSettingsService;

    public boolean hasAdmin(String userName, String site)
    {
        return hasRole(userName, "ROLE_ADMIN", "ROLE_ADMIN:" + site);
    }

    public boolean hasAdmin(String userName)
    {
        return hasRole(userName, "ROLE_ADMIN");
    }

    public boolean hasRole(String userName, String... requiredRoles) {
        User user = userService.getUser(userName);
        Set<String> roles = user.roles.stream().map(ur -> ur.role).collect(Collectors.toSet());
        for (String role : requiredRoles) {
            if (roles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasSelfReg() {
        GlobalSettings settings = globalSettingsService.getSettings();
        if (!BooleanUtils.isTrue((Boolean) settings.settings.get(GlobalSettings.ENABLE_SELF_REG))) {
            return false;
        }
        return true;
    }

    @PostMapping("regenLinkTable/{site}")
    @PreAuthorize("@adminController.hasAdmin(#principal.getName(), #site)")
    public ResponseEntity<Void> regenLinkTable(@PathVariable("site") String site, Principal principal) {
        regenCacheService.regenLinks(site);
        return ResponseEntity.ok().build();
    }

    @PostMapping("regenCacheTable/{site}")
    @PreAuthorize("@adminController.hasAdmin(#principal.getName(), #site)")
    public ResponseEntity<Void> regenCacheTable(@PathVariable("site") String site, Principal principal) {
        regenCacheService.regenCache(site);
        return ResponseEntity.ok().build();
    }

    @GetMapping("getUsers")
    @PreAuthorize("@adminController.hasRole(#principal.getName(), 'ROLE_ADMIN', 'ROLE_USERADMIN')")
    public ResponseEntity<List<UserDTO>> getUsers(Principal principal) {
        return ResponseEntity.ok(userService.getUsers());
    }

    @DeleteMapping("role/{userName}/{userRole}")
    @PreAuthorize("@adminController.hasAdmin(#principal.getName())")
    public ResponseEntity<UserDTO> deleteRole(Principal principal, @PathVariable("userName") String userName, @PathVariable("userRole") String userRole) {
        User user = userService.getUser(principal.getName());
        if (user.userName.equals(userName) && userRole.equals("ROLE_ADMIN")) {
            // Cannot remove your own admin role
            throw new AuthorizationDeniedException("Cannot remove your own admin role", new AuthorizationDecision(false));
        }
        return ResponseEntity.ok(userService.deleteRole(userName, userRole, user));
    }

    @PutMapping("role/{userName}/{userRole}")
    @PreAuthorize("@adminController.hasAdmin(#principal.getName())")
    public ResponseEntity<UserDTO> addRole(Principal principal, @PathVariable("userName") String userName, @PathVariable("userRole") String userRole) {
        User user = userService.getUser(principal.getName());
        return ResponseEntity.ok(userService.addRole(userName, userRole, user));
    }

    @PutMapping("roles/{userName}/site/{site}")
    @PreAuthorize("@adminController.hasAdmin(#principal.getName(), #site)")
    public ResponseEntity<UserDTO> setSiteRoles(Principal principal, @PathVariable("userName") String userName, @PathVariable("site") String site, @RequestBody List<String> siteRoles) {
        User user = userService.getUser(principal.getName());
        Set<String> roles = user.roles.stream().map(ur -> ur.role).collect(Collectors.toSet());
        if (siteRoles.stream().anyMatch(role -> {
            String[] parts = role.split(":");
            return parts.length != 3 || !parts[1].equals(site);
        })) {
            throw new AuthorizationDeniedException("Cannot remove your own admin role", new AuthorizationDecision(false));
        }
        return ResponseEntity.ok(userService.setSiteRoles(userName, site, siteRoles, user));
    }

    @PutMapping("user/{userName}")
    @PreAuthorize("@adminController.hasSelfReg() || #principal != null && @adminController.hasRole(#principal.getName(), 'ROLE_ADMIN', 'ROLE_USERADMIN')")
    public ResponseEntity<?> addUser(Principal principal, @PathVariable("userName") String userName, @RequestBody UserRequest userRequest) {
        User user = principal == null ? null : userService.getUser(principal.getName());
        if (userService.getUser(userName) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("User %s already exists".formatted(userName));
        }
        userService.addUser(userName, userRequest.password(), user, List.of(LazerWikiAuthenticationManager.USER));
        User u = userService.getUser(userName);
        UserDTO dto = new UserDTO(u.userName, null, u.roles.stream().map(role -> role.role).toList(), u.getSettings());
        return ResponseEntity.ok(dto);
    }

    @PostMapping("passwordReset/{userName}")
    @PreAuthorize("@adminController.hasRole(#principal.getName(), 'ROLE_ADMIN', 'ROLE_USERADMIN')")
    public ResponseEntity<Void> resetPassword(Principal principal, @PathVariable("userName") String userName, @RequestBody UserRequest userRequest) {
        userService.resetPassword(userName, userRequest.password());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("user/{userName}")
    @PreAuthorize("@adminController.hasAdmin(#principal.getName())")
    public ResponseEntity<Void> deleteUser(Principal principal, @PathVariable("userName") String userName) {
        User user = userService.getUser(principal.getName());
        userService.deleteUser(userName, user);
        return ResponseEntity.ok().build();
    }

    @GetMapping("sites")
    public List<Site> getAllSites(Principal principal) {
        User user = userService.getUser(principal.getName());
        return siteService.getAllSites(user);
    }

    @PutMapping("site/{siteName}")
    @PreAuthorize("@adminController.hasAdmin(#principal.getName())")
    public ResponseEntity<List<Site>> addSite(Principal principal, @PathVariable("siteName") String siteName, @RequestBody SiteRequest siteRequest) throws PageWriteException, IOException {
        User user = userService.getUser(principal.getName());
        String siteKey = siteRequest.name().toLowerCase();
        if (siteService.addSite(siteKey, siteRequest.hostName(), siteRequest.siteName() )) {
            pageUpdateService.createDefaultSiteHomepage(siteKey, siteRequest.siteName(), user.userName);
        }
        return ResponseEntity.ok(siteService.getAllSites(user));
    }

    @PostMapping("site/settings/{siteName}")
    @PreAuthorize("@adminController.hasAdmin(#principal.getName(), #siteName)")
    public ResponseEntity<SiteSettingsResponse> setSiteSettings(Principal principal, @PathVariable("siteName") String siteName, @RequestBody SiteSettingsRequest siteRequest) throws PageWriteException, IOException {
        User user = userService.getUser(principal.getName());
        try {
            Site site = siteService.setSiteSettings(siteName, siteRequest.hostName(), siteRequest.siteSettings(), user);
            return ResponseEntity.ok(new SiteSettingsResponse(site, true, ""));
        }
        catch(SiteSettingsException e) {
            return ResponseEntity.ok(new SiteSettingsResponse(null, false, e.getMessage()));
        }
    }

    @PostMapping("namespace/restrictionType")
    @PreAuthorize("@adminController.hasAdmin(#principal.getName(), #restrictionRequest.site())")
    public ResponseEntity<PageListResponse> setNamespaceRestrictionType(Principal principal, @RequestBody NamespaceRestrictionRequest restrictionRequest) throws PageWriteException, IOException {
        User user = userService.getUser(principal.getName());
        namespaceService.setNSRestriction(restrictionRequest.site(), restrictionRequest.namespace(), restrictionRequest.restrictionType());
        return ResponseEntity.ok(pageService.getAllNamespaces(restrictionRequest.site(), user.userName));
    }

    @DeleteMapping("site/{siteName}")
    @PreAuthorize("@adminController.hasAdmin(#principal.getName())")
    public ResponseEntity<List<Site>> deleteSite(Principal principal, @PathVariable("siteName") String siteName) throws PageWriteException, IOException, MediaWriteException {
        User user = userService.getUser(principal.getName());
        Set<String> roles = user.roles.stream().map(ur -> ur.role).collect(Collectors.toSet());
        siteDelService.deleteSiteCompletely(siteName, principal.getName());
        return ResponseEntity.ok(siteService.getAllSites(user));
    }

    @GetMapping("globalSettings")
    public ResponseEntity<GlobalSettings> getGlobalSettings(Principal principal) {
        return ResponseEntity.ok(globalSettingsService.getSettings());
    }

    @PostMapping("globalSettings")
    @PreAuthorize("@adminController.hasAdmin(#principal.getName())")
    public ResponseEntity<CommonResponse> setGlobalSettings(Principal principal, @RequestBody GlobalSettings settings) {
        globalSettingsService.setSettings(settings);
        return ResponseEntity.ok(new CommonResponse(true, null));
    }
}
