package us.calubrecht.lazerwiki;

import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.*;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.*;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity()
public class WebSecurityConfig {
  @Autowired AuthenticationManager authenticationManager;

  @Value("${webserver.frontend:}")
  String webserverFrontEnd;

  @Value("${webserver.urlprefix}")
  String urlPrefix;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) {
    CookieCsrfTokenRepository cookieRepo = CookieCsrfTokenRepository.withHttpOnlyFalse();
    cookieRepo.setCookieCustomizer(
        c -> {
          c.sameSite("Strict");
        });
    RequestMatcher createAdminMatcher = getMatcher("/specialAdmin/createNewAdmin");
    http.csrf(
        (csrf) ->
            csrf.ignoringRequestMatchers(createAdminMatcher)
                .csrfTokenRepository(cookieRepo)
                .csrfTokenRequestHandler(new XorCsrfTokenRequestAttributeHandler()::handle));
    http.authorizeHttpRequests(
        (authz) -> {
          authz
              .requestMatchers(
                  getMatcher("/api/sessions/login"),
                  getMatcher("/error"),
                  getMatcher("/api/version"),
                  getMatcher("/api/csrf"),
                  getMatcher("/_media/**", HttpMethod.OPTIONS),
                  getMatcher("/_media/**", HttpMethod.GET),
                  getMatcher("/_resources/**"),
                  getMatcher("/api/page/get/*", HttpMethod.GET),
                  getMatcher("/api/page/getHistorical/**", HttpMethod.GET),
                  getMatcher("/api/page/history/*", HttpMethod.GET),
                  getMatcher("/api/page/diff/**", HttpMethod.GET),
                  getMatcher("/api/page/listPages", HttpMethod.GET),
                  getMatcher("/api/page/listTags", HttpMethod.GET),
                  getMatcher("/api/page/searchPages", HttpMethod.GET),
                  getMatcher("/api/page/recentChanges", HttpMethod.GET),
                  getMatcher("/api/history/recentChanges", HttpMethod.GET),
                  getMatcher("/api/site/**", HttpMethod.GET),
                  getMatcher("/api/plugin/**", HttpMethod.GET),
                  getMatcher("/api/settings/globalSettings", HttpMethod.GET),
                  getMatcher("/api/admin/user/*", HttpMethod.PUT),
                  getMatcher("/api/users/resetForgottenPassword", HttpMethod.POST),
                  getMatcher("/api/users/verifyPasswordToken", HttpMethod.POST),
                  getMatcher("/*", HttpMethod.GET),
                  getMatcher("/assets/*", HttpMethod.GET),
                  getMatcher("/page/*", HttpMethod.GET),
                  getMatcher("/api/page/savePage", HttpMethod.POST),
                  getMatcher("/api/page/*/savePage", HttpMethod.POST),
                  getMatcher("/api/page/lock/**", HttpMethod.POST),
                  getMatcher("/sitemap.xml", HttpMethod.GET),
                  createAdminMatcher)
              .permitAll()
              .
              // default
              anyRequest()
              .authenticated();
        });
    LazerWikiAuthenticationFilter filter =
        new LazerWikiAuthenticationFilter(authenticationManager, webserverFrontEnd, urlPrefix);
    http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  RequestMatcher getMatcher(String path, HttpMethod method) {
    return PathPatternRequestMatcher.withDefaults().matcher(method, path);
  }

  RequestMatcher getMatcher(String path) {
    return PathPatternRequestMatcher.withDefaults().matcher(path);
  }
}
