package us.calubrecht.lazerwiki.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class LazerwikiEmailConfig {
    @Value("${lazerwiki.email.smtp.host}")
    String host;

    @Value("${lazerwiki.email.smtp.user}")
    String user;

    @Value("${lazerwiki.email.smtp.passwd}")
    String passwd;

/*    @Bean
    MailSender getMailSender() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setUsername(user);
        sender.setPassword(passwd);
        sender.setPort(587);
        sender.getJavaMailProperties().s
        return sender;
    }*/
}
