package us.calubrecht.lazerwiki;

import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

@Component
public final class LazerWikiAuthenticationManager implements AuthenticationManager {
  final Logger logger = LogManager.getLogger(getClass());

  @Autowired UserService userService;

  public static final GrantedAuthority USER = new SimpleGrantedAuthority("ROLE_USER");
  public static final GrantedAuthority ADMIN = new SimpleGrantedAuthority("ROLE_ADMIN");

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    if (!(authentication instanceof UsernamePasswordAuthenticationToken token)) {
      throw new BadCredentialsException("Invalid authentication");
    }
    String name = token.getName();
    try {
      User u = userService.getUser(name);
      if (u != null) {
        Object credentials = token.getCredentials();
        if (credentials != null && userService.verifyPassword(u, credentials.toString())) {
          return new UsernamePasswordAuthenticationToken(
              token.getName(),
              null,
              u.roles.stream()
                  .map(role -> new SimpleGrantedAuthority(role.role))
                  .collect(Collectors.toList()));
        }
      }
    } catch (BadCredentialsException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Exception during authentication", e);
      throw new BadCredentialsException("Invalid authentication");
    }

    throw new BadCredentialsException("Invalid authentication");
  }
}
