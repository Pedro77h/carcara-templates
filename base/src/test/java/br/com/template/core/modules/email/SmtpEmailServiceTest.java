package br.com.template.core.modules.email;

import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class SmtpEmailServiceTest {

    @Mock
    private JavaMailSender javaMailSender;

    private SmtpEmailService smtpEmailService;

    @BeforeEach
    void setUp() {
        smtpEmailService = new SmtpEmailService(javaMailSender, "core");
    }

    private MimeMessage buildMimeMessage() {
        return new MimeMessage(Session.getInstance(new Properties()));
    }

    private String extractText(Part part) throws Exception {
        Object content = part.getContent();

        if (content instanceof MimeMultipart multipart) {
            StringBuilder text = new StringBuilder();
            for (int index = 0; index < multipart.getCount(); index++) {
                text.append(extractText(multipart.getBodyPart(index)));
            }
            return text.toString();
        }

        return content.toString();
    }

    @Test
    @DisplayName("should send the recovery email addressed to the user")
    void shouldSendRecoveryMail() throws Exception {
        MimeMessage mimeMessage = buildMimeMessage();
        given(javaMailSender.createMimeMessage()).willReturn(mimeMessage);

        smtpEmailService.sendRecoveryMail("maria@example.com", "http://localhost:3000/reset?token=abc", 30);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        then(javaMailSender).should().send(captor.capture());
        assertThat(captor.getValue().getAllRecipients()[0]).hasToString("maria@example.com");
        assertThat(captor.getValue().getSubject()).isEqualTo("Recuperação de senha - core");
        assertThat(extractText(captor.getValue()))
                .contains("http://localhost:3000/reset?token=abc")
                .contains("30 minutos");
    }

    @Test
    @DisplayName("should fail loudly when the mail server rejects the message")
    void shouldFail_whenMailServerRejects() {
        MimeMessage mimeMessage = buildMimeMessage();
        given(javaMailSender.createMimeMessage()).willReturn(mimeMessage);
        willThrow(new org.springframework.mail.MailSendException("smtp down")).given(javaMailSender).send(mimeMessage);

        assertThatThrownBy(() -> smtpEmailService.sendRecoveryMail("maria@example.com", "http://link", 30))
                .isInstanceOf(org.springframework.mail.MailSendException.class);
    }

    @Test
    @DisplayName("should send the email verification message with the confirmation link")
    void shouldSendEmailVerificationMail() {
        MimeMessage mimeMessage = new MimeMessage((jakarta.mail.Session) null);
        given(javaMailSender.createMimeMessage()).willReturn(mimeMessage);

        smtpEmailService.sendEmailVerificationMail("maria@example.com",
                "http://localhost:3000/verificar-email?token=abc", 1440);

        then(javaMailSender).should().send(mimeMessage);
    }

    @Test
    @DisplayName("should warn the owner that someone tried to register with their email")
    void shouldSendRegistrationAttemptMail() {
        MimeMessage mimeMessage = new MimeMessage((jakarta.mail.Session) null);
        given(javaMailSender.createMimeMessage()).willReturn(mimeMessage);

        smtpEmailService.sendRegistrationAttemptMail("maria@example.com");

        then(javaMailSender).should().send(mimeMessage);
    }
}
