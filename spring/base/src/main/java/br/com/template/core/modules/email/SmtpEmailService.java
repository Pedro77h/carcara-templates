package br.com.template.core.modules.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class SmtpEmailService {

    private static final String PASSWORD_RESET_TEMPLATE_PATH = "templates/email/password-reset.html";
    private static final String EMAIL_VERIFICATION_TEMPLATE_PATH = "templates/email/email-verification.html";
    private static final String REGISTRATION_ATTEMPT_TEMPLATE_PATH = "templates/email/registration-attempt.html";

    private final JavaMailSender javaMailSender;
    private final String appName;

    public SmtpEmailService(JavaMailSender javaMailSender,
                            @Value("${app.name:${spring.application.name}}") String appName) {
        this.javaMailSender = javaMailSender;
        this.appName = appName;
    }

    public void sendRecoveryMail(String userEmail, String resetLink, long expirationMinutes) {
        try {
            MimeMessage message = buildMessage(userEmail, "Recuperação de senha - " + appName,
                    PASSWORD_RESET_TEMPLATE_PATH, resetLink, expirationMinutes);
            javaMailSender.send(message);
        } catch (MessagingException | IOException ex) {
            throw new IllegalStateException("Failed to send password reset email", ex);
        }
    }

    public void sendEmailVerificationMail(String userEmail, String verificationLink, long expirationMinutes) {
        try {
            MimeMessage message = buildMessage(userEmail, "Confirme seu e-mail - " + appName,
                    EMAIL_VERIFICATION_TEMPLATE_PATH, verificationLink, expirationMinutes);
            javaMailSender.send(message);
        } catch (MessagingException | IOException ex) {
            throw new IllegalStateException("Failed to send email verification email", ex);
        }
    }

    public void sendRegistrationAttemptMail(String userEmail) {
        try {
            MimeMessage message = buildMessage(userEmail, "Tentativa de cadastro - " + appName,
                    REGISTRATION_ATTEMPT_TEMPLATE_PATH, "", 0);
            javaMailSender.send(message);
        } catch (MessagingException | IOException ex) {
            throw new IllegalStateException("Failed to send registration attempt email", ex);
        }
    }

    private MimeMessage buildMessage(String userEmail, String subject, String templatePath, String actionLink,
                                     long expirationMinutes) throws MessagingException, IOException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setTo(userEmail);
        helper.setSubject(subject);
        helper.setText(buildHtml(templatePath, actionLink, expirationMinutes), true);

        return mimeMessage;
    }

    private String buildHtml(String templatePath, String actionLink, long expirationMinutes) throws IOException {
        String template = StreamUtils.copyToString(
                new ClassPathResource(templatePath).getInputStream(), StandardCharsets.UTF_8);

        return template
                .replace("{{APP_NAME}}", appName)
                .replace("{{ACTION_LINK}}", actionLink)
                .replace("{{EXPIRATION_MINUTES}}", String.valueOf(expirationMinutes));
    }
}
