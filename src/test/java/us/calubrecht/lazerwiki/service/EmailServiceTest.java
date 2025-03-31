package us.calubrecht.lazerwiki.service;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = EmailService.class)
@ActiveProfiles("test")
class EmailServiceTest {

    @Autowired
    EmailService underTest;

    @MockBean
    JavaMailSender mailSender;

    @Test
    void sendEmail() throws MessagingException {
        MimeMessage message = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(message);
        underTest.sendEmail("host", "a@a.com", "A User", "Subject", "The body");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());

        assertEquals("The Admin <a@b.com>", captor.getValue().getFrom()[0].toString());
        assertEquals("A User <a@a.com>", captor.getValue().getRecipients(MimeMessage.RecipientType.TO)[0].toString());

        underTest.srcName = "";
        underTest.sendEmail("host", "a@a.com", "A User", "Subject", "The body");

        verify(mailSender, times(2)).send(captor.capture());

        assertEquals("a@b.com", captor.getValue().getFrom()[0].toString());
        assertEquals("A User <a@a.com>", captor.getValue().getRecipients(MimeMessage.RecipientType.TO)[0].toString());


    }
}