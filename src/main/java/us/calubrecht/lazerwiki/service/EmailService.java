package us.calubrecht.lazerwiki.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    @Value("${lazerwiki.email.smtp.src.email}")
    String srcEmail;

    @Value("${lazerwiki.email.smtp.src.name}")
    String srcName;

    @Autowired
    JavaMailSender mailSender;

    public void sendEmail(String host, String destEmail, String destUser, String subject, String body) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
        String fullSrc = srcName.isBlank() ? srcEmail : "%s <%s>".formatted(srcName, srcEmail);
        helper.setFrom(fullSrc);
        helper.setTo("%s <%s>".formatted(destUser, destEmail));
        helper.setText(body, true);
        helper.setSubject(subject);
        mailSender.send(mimeMessage);
    }
}
