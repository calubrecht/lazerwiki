package us.calubrecht.lazerwiki;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

public class LazerWikiAuthenticationFilter  extends AbstractAuthenticationProcessingFilter {

    protected LazerWikiAuthenticationFilter(String defaultFilterProcessesUrl, AuthenticationManager mgr, String webserverFrontEnd, String urlPrefix)
    {
        super(
                new MyMatcher(defaultFilterProcessesUrl));
        setAuthenticationManager(mgr);
        setSessionAuthenticationStrategy(new ChangeSessionIdAuthenticationStrategy());
        setAuthenticationSuccessHandler(new ForwardSuccessHandler(webserverFrontEnd,  urlPrefix + "/api/sessions/username"));
        setSecurityContextRepository(new HttpSessionSecurityContextRepository());

    }
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException, ServletException {
        if (!request.getMethod().equals("POST")) {
            throw new AuthenticationServiceException(
                    "Authentication method not supported: " + request.getMethod());
        }

        UsernamePasswordAuthenticationToken authRequest = getAuthToken(request);

        authRequest.setDetails(request);

        return this.getAuthenticationManager().authenticate(authRequest);
    }

    protected UsernamePasswordAuthenticationToken getAuthToken(HttpServletRequest request) throws IOException
    {
        String sData = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        JsonParser springParser = JsonParserFactory.getJsonParser();
        Map<String, Object> map = springParser.parseMap(sData);
        String username = map.get("username").toString();
        String password = map.get("password").toString();

        // TODO Auto-generated method stub
        UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(
                username, password);
        return authRequest;
    }

    private static class ForwardSuccessHandler extends SimpleUrlAuthenticationSuccessHandler
    {
        String url;

        public ForwardSuccessHandler(String defaultHost, String url)
        {
            super(defaultHost + url);
            this.url = url;
        }

        @Override
        public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                            Authentication authentication) throws IOException, ServletException
        {
            response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
            response.setHeader("Access-Control-Allow-Credentials", "true");
            super.onAuthenticationSuccess(request, response, authentication);

        }

        private String stripTrailingSlash(String url)
        {
            if (url.endsWith("/"))
            {
                return url.substring(0, url.length() -1);
            }
            return url;
        }

        protected String determineTargetUrl(HttpServletRequest request,
                                            HttpServletResponse response)
        {
            Object oReferer = request.getHeader("Referer");
            if (oReferer != null)
            {
                String referer = stripTrailingSlash((String)oReferer);
                return referer + url;
            }
            return super.determineTargetUrl(request, response);
        }

    }

    public static class MyMatcher implements RequestMatcher
    {
        private RequestMatcher delegate_;
        public MyMatcher(String defaultFilterProcessesUrl)
        {
            delegate_= new AndRequestMatcher(
                    new AntPathRequestMatcher(defaultFilterProcessesUrl),
                    new NegatedRequestMatcher(new AntPathRequestMatcher(defaultFilterProcessesUrl, "OPTIONS")));
        }

        @Override
        public boolean matches(HttpServletRequest request)
        {
            // TODO Auto-generated method stub
            return delegate_.matches(request);
        }

    }
}
