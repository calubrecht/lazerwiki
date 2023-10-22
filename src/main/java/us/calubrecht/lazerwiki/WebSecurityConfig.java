package us.calubrecht.lazerwiki;


import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.*;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.*;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {
    @Autowired
    AuthenticationManager authenticationManager;

    @Value("${webserver.frontend:}")
    String webserverFrontEnd;

    @Value("${webserver.urlprefix}")
    String urlPrefix;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
           // http.csrf().csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());
            http.csrf().disable();
            http
                    .authorizeHttpRequests((authz) -> {
                        authz.requestMatchers(
                                        new AntPathRequestMatcher("/api/sessions/login"),
                                        new AntPathRequestMatcher("/error"),
                                        new AntPathRequestMatcher("/api/version"),
                                        // Ignore for CORS requests
                                        new AntPathRequestMatcher("/**", HttpMethod.OPTIONS.toString())).permitAll().
                        requestMatchers( new AntPathRequestMatcher("/api/page/**")).permitAll().
                        // default
                        anyRequest().authenticated();
        }
                    );
          LazerWikiAuthenticationFilter filter = new LazerWikiAuthenticationFilter("/api/sessions/login", authenticationManager, webserverFrontEnd, urlPrefix);
          http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);
            return http.build();
        }
}
