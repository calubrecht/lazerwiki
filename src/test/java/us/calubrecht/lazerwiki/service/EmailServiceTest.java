package us.calubrecht.lazerwiki.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = EmailService.class)
@ActiveProfiles("test")
class EmailServiceTest {

  @Autowired EmailService underTest;

  @MockitoBean JavaMailSender mailSender;

  @Test
  void test_sendEmail() throws MessagingException {
    MimeMessage message = new MimeMessage((Session) null);
    when(mailSender.createMimeMessage()).thenReturn(message);
    underTest.sendEmail("a@a.com", "A User", "Subject", "The body");

    ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
    verify(mailSender).send(captor.capture());

    assertEquals("The Admin <a@b.com>", captor.getValue().getFrom()[0].toString());
    assertEquals(
        "A User <a@a.com>",
        captor.getValue().getRecipients(MimeMessage.RecipientType.TO)[0].toString());

    underTest.srcName = "";
    underTest.sendEmail("a@a.com", "A User", "Subject", "The body");

    verify(mailSender, times(2)).send(captor.capture());

    assertEquals("a@b.com", captor.getValue().getFrom()[0].toString());
    assertEquals(
        "A User <a@a.com>",
        captor.getValue().getRecipients(MimeMessage.RecipientType.TO)[0].toString());
  }
}
