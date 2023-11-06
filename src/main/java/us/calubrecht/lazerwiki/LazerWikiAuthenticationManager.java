package us.calubrecht.lazerwiki;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.model.User;
import us.calubrecht.lazerwiki.service.UserService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


@Component
public class LazerWikiAuthenticationManager implements AuthenticationManager {
    Log logger = LogFactory.getLog(getClass());

    @Autowired
    UserService userService;

    public static final GrantedAuthority USER = new SimpleGrantedAuthority("ROLE_USER");
    public static final GrantedAuthority ADMIN = new SimpleGrantedAuthority("ROLE_ADMIN");

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (!(authentication instanceof UsernamePasswordAuthenticationToken)) {
            throw new BadCredentialsException("Invalid authentication");
        }
        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) authentication;
        String name = token.getName();
        try {
            User u = userService.getUser(name);
            if (u != null ) {
                if (userService.verifyPassword(u, token.getCredentials().toString())) {
                    return new UsernamePasswordAuthenticationToken(token.getName(), null, u.roles.stream().map(role -> new SimpleGrantedAuthority(role.role)).collect(Collectors.toList()));
                }
            }
        }
        catch (BadCredentialsException e) {
            throw e;
        }
        catch (Exception e) {
            logger.error("Exception during authentication", e);
            throw new BadCredentialsException("Invalid authentication");
        }

        throw new BadCredentialsException("Invalid authentication");
    }

}
