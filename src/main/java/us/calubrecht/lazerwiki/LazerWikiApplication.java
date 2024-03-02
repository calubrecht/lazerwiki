package us.calubrecht.lazerwiki;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import us.calubrecht.lazerwiki.controller.VersionController;

@SpringBootApplication
@ServletComponentScan
@EnableCaching
public class LazerWikiApplication  extends SpringBootServletInitializer {
	final Logger logger = LoggerFactory.getLogger(getClass());

	public static void main(String[] args) {
		SpringApplication.run(LazerWikiApplication.class, args);
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application)
	{
		return application.sources(LazerWikiApplication.class);
	}

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		super.onStartup(servletContext);
		servletContext.getSessionCookieConfig().setMaxAge(90 * 24 * 60 * 60);
		servletContext.getSessionCookieConfig().setSecure(true);
		servletContext.setSessionTimeout(90 * 24 * 60);
		logger.info("Started Lazerwiki Application  v:{}",   VersionController.getVersion());
	}

	@Bean
	public ServletListenerRegistrationBean<LogoutListener> sessionListenerWithMetrics() {
		ServletListenerRegistrationBean<LogoutListener> listenerRegBean =
				new ServletListenerRegistrationBean<>();

		listenerRegBean.setListener(new LogoutListener());
		return listenerRegBean;
	}

	 class LogoutListener implements HttpSessionListener {
		@Override
		public void sessionDestroyed(HttpSessionEvent event) {
			HttpSession session = event.getSession();
			String username = (String)session.getAttribute("username");
			if (null != username) {
				logger.warn("Unexpected session destroyed: {}-{}", session.getId(), username);
			}
		}
	}
}
