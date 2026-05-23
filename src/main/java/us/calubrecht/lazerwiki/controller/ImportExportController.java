package us.calubrecht.lazerwiki.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import us.calubrecht.lazerwiki.model.User;
import us.calubrecht.lazerwiki.service.ExportService;
import us.calubrecht.lazerwiki.service.SiteService;
import us.calubrecht.lazerwiki.service.UserService;

import java.security.Principal;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/io/")
public class ImportExportController {

    @Autowired
    UserService userService;

    @Autowired
    ExportService exportService;

    @Autowired
    SiteService siteService;

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

    @GetMapping("export/{site}")
    @PreAuthorize("@adminController.hasAdmin(#principal.getName(), #site)")
    public ResponseEntity<Void> exportSite(@PathVariable("site") String site, Principal principal) {
        exportService.createExportBundle(siteService.getHostForSitename(site), principal.getName());
        return ResponseEntity.ok().build();
    }
}
