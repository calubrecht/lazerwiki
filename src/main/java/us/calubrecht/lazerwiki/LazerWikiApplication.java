package us.calubrecht.lazerwiki;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.springframework.context.event.EventListener;
import us.calubrecht.lazerwiki.controller.VersionController;

@SpringBootApplication
public class LazerWikiApplication  extends SpringBootServletInitializer {
	Logger logger = LoggerFactory.getLogger(getClass());

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
		logger.info("Started Lazerwiki Application  v:{}",   VersionController.getVersion());
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onAppStartup() throws ServletException {
		logger.info("Started Lazerwiki Application  v:{}",   VersionController.getVersion());
	}
}
