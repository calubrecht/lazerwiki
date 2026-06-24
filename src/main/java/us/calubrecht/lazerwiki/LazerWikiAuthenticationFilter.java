package us.calubrecht.lazerwiki;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

public class LazerWikiAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

  protected LazerWikiAuthenticationFilter(
      AuthenticationManager mgr, String webserverFrontEnd, String urlPrefix) {
    super(new MyMatcher("/api/sessions/login"));
    setAuthenticationManager(mgr);
    setSessionAuthenticationStrategy(new ChangeSessionIdAuthenticationStrategy());
    setAuthenticationSuccessHandler(
        new ForwardSuccessHandler(webserverFrontEnd, urlPrefix + "/api/sessions/username"));
    setSecurityContextRepository(new HttpSessionSecurityContextRepository());
  }

  @Override
  public Authentication attemptAuthentication(
      HttpServletRequest request, @NonNull HttpServletResponse response)
      throws AuthenticationException, IOException {
    if (!request.getMethod().equals("POST")) {
      throw new AuthenticationServiceException(
          "Authentication method not supported: " + request.getMethod());
    }

    UsernamePasswordAuthenticationToken authRequest = getAuthToken(request);

    authRequest.setDetails(request);

    return this.getAuthenticationManager().authenticate(authRequest);
  }

  protected UsernamePasswordAuthenticationToken getAuthToken(HttpServletRequest request)
      throws IOException {
    String sData = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
    JsonParser springParser = JsonParserFactory.getJsonParser();
    Map<String, Object> map = springParser.parseMap(sData);
    String username = map.get("username").toString();
    String password = map.get("password").toString();

    return new UsernamePasswordAuthenticationToken(username, password);
  }

  private static class ForwardSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    final String url;

    public ForwardSuccessHandler(String defaultHost, String url) {
      super(defaultHost + url);
      this.url = url;
    }

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request, HttpServletResponse response, Authentication authentication)
        throws IOException, ServletException {
      request.getSession(false).setMaxInactiveInterval(90 * 24 * 60 * 60);
      request.getSession(false).setAttribute("username", authentication.getPrincipal());
      super.onAuthenticationSuccess(request, response, authentication);
    }

    protected @NonNull String determineTargetUrl(
        HttpServletRequest request, @NonNull HttpServletResponse response) {
      String oReferer = (String) request.getHeader("Referer");

      try {
        if (oReferer != null) {
          URL referredUrl = new URI(oReferer).toURL();
          return new URI(
                  referredUrl.getProtocol(),
                  null,
                  referredUrl.getHost(),
                  referredUrl.getPort(),
                  url,
                  null,
                  null)
              .toURL()
              .toString();
        }
      } catch (MalformedURLException | URISyntaxException e) {
        return url;
      }
      return super.determineTargetUrl(request, response);
    }
  }

  public static class MyMatcher implements RequestMatcher {
    private final RequestMatcher delegate;

    public MyMatcher(String defaultFilterProcessesUrl) {
      RequestMatcher defaultFilter =
          PathPatternRequestMatcher.withDefaults().matcher(defaultFilterProcessesUrl);
      RequestMatcher optionsFilter =
          PathPatternRequestMatcher.withDefaults()
              .matcher(HttpMethod.OPTIONS, defaultFilterProcessesUrl);
      delegate = new AndRequestMatcher(defaultFilter, new NegatedRequestMatcher(optionsFilter));
    }

    @Override
    public boolean matches(@NonNull HttpServletRequest request) {
      return delegate.matches(request);
    }
  }
}
///
