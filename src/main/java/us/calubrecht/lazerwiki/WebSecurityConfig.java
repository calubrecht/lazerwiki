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
import org.springframework.security.web.util.matcher.*;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig {
    @Autowired
    AuthenticationManager authenticationManager;

    @Value("${webserver.frontend:}")
    String webserverFrontEnd;

    @Value("${webserver.urlprefix}")
    String urlPrefix;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository cookieRepo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        cookieRepo.setCookieCustomizer(
                c -> {
                    c.sameSite("Strict");

                });
        RequestMatcher createAdminMatcher = new AntPathRequestMatcher("/specialAdmin/createNewAdmin");
        http.csrf((csrf) -> csrf
                .ignoringRequestMatchers(createAdminMatcher)
                .csrfTokenRepository(cookieRepo)
                .csrfTokenRequestHandler(new XorCsrfTokenRequestAttributeHandler()::handle)
        );
        http
                .authorizeHttpRequests((authz) -> {
                            authz.requestMatchers(
                                            new AntPathRequestMatcher("/api/sessions/login"),
                                            new AntPathRequestMatcher("/error"),
                                            new AntPathRequestMatcher("/api/version"),
                                            new AntPathRequestMatcher("/api/csrf"),
                                            new AntPathRequestMatcher("/_media/**", HttpMethod.OPTIONS.toString()),
                                            new AntPathRequestMatcher("/_media/**", HttpMethod.GET.toString()),
                                            new AntPathRequestMatcher("/_resources/**"),
                                            new AntPathRequestMatcher("/api/page/get/*", HttpMethod.GET.toString()),
                                            new AntPathRequestMatcher("/api/page/getHistorical/**", HttpMethod.GET.toString()),
                                            new AntPathRequestMatcher("/api/page/history/*", HttpMethod.GET.toString()),
                                            new AntPathRequestMatcher("/api/page/diff/**", HttpMethod.GET.toString()),
                                            new AntPathRequestMatcher("/api/page/listPages", HttpMethod.GET.toString()),
                                            new AntPathRequestMatcher("/api/page/listTags", HttpMethod.GET.toString()),
                                            new AntPathRequestMatcher("/api/page/searchPages", HttpMethod.GET.toString()),
                                            new AntPathRequestMatcher("/api/page/recentChanges", HttpMethod.GET.toString()),
                                            new AntPathRequestMatcher("/api/history/recentChanges", HttpMethod.GET.toString()),
                                            new AntPathRequestMatcher("/api/site/**", HttpMethod.GET.toString()),
                                            new AntPathRequestMatcher("/api/plugin/**", HttpMethod.GET.toString()),
                                            new AntPathRequestMatcher("/api/admin/globalSettings", HttpMethod.GET.toString()),
                                            new AntPathRequestMatcher("/api/admin/user/*", HttpMethod.PUT.toString()),
                                            new AntPathRequestMatcher("/api/users/resetForgottenPassword", HttpMethod.POST.toString()),
                                            new AntPathRequestMatcher("/api/users/verifyPasswordToken", HttpMethod.POST.toString()),
                                            new AntPathRequestMatcher("/*", HttpMethod.GET.toString()),
                                            new AntPathRequestMatcher("/assets/*", HttpMethod.GET.toString()),
                                            new AntPathRequestMatcher("/page/*", HttpMethod.GET.toString()),
                                            // Ignore for CORS requests
                                            new AntPathRequestMatcher("/**", HttpMethod.OPTIONS.toString()),
                                            createAdminMatcher).permitAll().
                                    // default
                                            anyRequest().authenticated();
                        }
                );
        LazerWikiAuthenticationFilter filter = new LazerWikiAuthenticationFilter("/api/sessions/login", authenticationManager, webserverFrontEnd, urlPrefix);
        http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    // APIPathRequestMatcher... do an or between 2 antsPathrequestmatchers with or without app
}
