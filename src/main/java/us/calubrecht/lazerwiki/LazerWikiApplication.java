package us.calubrecht.lazerwiki;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

@SpringBootApplication
public class LazerWikiApplication  extends SpringBootServletInitializer {
	Logger logger = LoggerFactory.getLogger(getClass());

	public static void main(String[] args) {
		SpringApplication.run(LazerWikiApplication.class, args);
	}


	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		super.onStartup(servletContext);
		logger.info("Started Lazerwiki Application  v:{}",  "version");// VersionController.getVersion());
	}
}
