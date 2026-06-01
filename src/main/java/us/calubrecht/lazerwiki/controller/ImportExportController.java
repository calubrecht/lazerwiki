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
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
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

    public boolean hasAdmin(String userName, String site)
    {
        return hasRole(userName, "ROLE_ADMIN", "ROLE_ADMIN:" + site);
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
    @PreAuthorize("@importExportController.hasAdmin(#principal.getName(), #site)")
    public ResponseEntity<StreamingResponseBody> exportSite(@PathVariable("site") String site, Principal principal) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment;filename=export-" + site + ".tar.gz")
                .body(outputStream -> {
                    try {
                        exportService.createExportBundle(
                                site,
                                principal.getName(),
                                outputStream
                        );
                        logger.info("Export streaming completed for site: {}", site);
                    } catch (IOException e) {
                        logger.error("Error during export streaming", e);
                        throw e;
                    }
                });
    }
}
