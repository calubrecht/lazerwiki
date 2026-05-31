package us.calubrecht.lazerwiki.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

import java.io.IOException;
import java.security.Principal;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/io/")
public class ImportExportController {
    final Logger logger = LogManager.getLogger(getClass());

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
    public ResponseEntity<byte[]> exportSite(@PathVariable("site") String site, Principal principal) throws IOException {
        byte[] output = exportService.createExportBundle(siteService.getHostForSitename(site), principal.getName());
        logger.info("Output size: {} bytes", output.length);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Disposition", "attachment;filename=export.tar.gz"); // Include site name
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).headers(headers).body(output);
    }
}
