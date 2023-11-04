package us.calubrecht.lazerwiki.controller;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import us.calubrecht.lazerwiki.LazerWikiAuthenticationManager;
import us.calubrecht.lazerwiki.service.UserService;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

/**
 * Important:
 * Access to this controller allows creation of admin users. It is intended for creation of the first admin
 * user who can then create other users as necessary.
 * To enable it, the property admin.user.creation.enabled should be set to true.
 * ONLY do this if only trusted users have access to the hosting machine.
 * If behind a reverse proxy (ex Nginx), make sure the header X-Real-IP added to all requests with the
 * source IP.
 * Likely a good idea to disable this property and restart service once user is created.
 */
@RestController()
@RequestMapping("specialAdmin/")
@ConditionalOnProperty(
        value = "admin.user.creation.enabled",
        havingValue = "true")
public class AdminUserController {
    Logger logger = LogManager.getLogger(getClass());
    @Autowired
    UserService userService;

    @PostMapping("createNewAdmin")
    public ResponseEntity<String> createNewAdmin(HttpServletRequest request, @RequestBody Map<String, String> body) {
        String realIPHeader = request.getHeader("X-Real-IP");
        if (realIPHeader != null) {
            // This request has been forwarded from a proxy. Deny.
            logger.error("Attempt to create Admin User '" + body.get("userName") + "' from forwarded " + realIPHeader);
            return ResponseEntity.notFound().build();
        }
        try {
            InetAddress remoteAddress = InetAddress.getByName(request.getRemoteAddr());
            if (!remoteAddress.isLoopbackAddress()) {
                // This request has come from a remote source.
                logger.error("Attempt to create Admin User '" + body.get("userName") + "' from " + remoteAddress);
                return ResponseEntity.notFound().build();
            }
            userService.addUser(body.get("userName"), body.get("password"), List.of(LazerWikiAuthenticationManager.USER, LazerWikiAuthenticationManager.ADMIN));
            return ResponseEntity.ok(body.get("userName") + " created");
        } catch (UnknownHostException e) {
            logger.error(e);
            return ResponseEntity.notFound().build();
        }
    }

}
