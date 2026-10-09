package br.com.template.core.modules.auth;

import br.com.template.core.modules.user.models.User;
import br.com.template.core.modules.user.repositories.UserRepository;
import br.com.template.core.support.CoreTestServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTest extends CoreTestServer {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String email;

    @BeforeEach
    void setUp() {
        email = "ana" + System.nanoTime() + "@example.com";
    }

    private int register(String email) throws Exception {
        String payload = """
                {
                  "fullName": "Ana Souza",
                  "phone": "85999990000",
                  "email": "%s",
                  "password": "senha-forte"
                }
                """.formatted(email);

        return mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andReturn().getResponse().getStatus();
    }

    private void confirmEmail(String email) {
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        user.setEmailVerifiedAt(OffsetDateTime.now());
        userRepository.save(user);
    }

    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).get("accessToken").asString();
    }

    private int loginStatus(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andReturn().getResponse().getStatus();
    }

    private int profileStatus(String accessToken) throws Exception {
        return mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andReturn().getResponse().getStatus();
    }

    @Test
    @DisplayName("should answer 202 on register and refuse login while the email was not confirmed")
    void shouldRefuseLoginBeforeEmailConfirmation() throws Exception {
        assertThat(register(email)).isEqualTo(202);
        assertThat(userRepository.findByEmailIgnoreCase(email)).isPresent();

        assertThat(loginStatus(email, "senha-forte")).isEqualTo(401);
    }

    @Test
    @DisplayName("should answer 202 and create no second account when the email is already taken")
    void shouldNeverRevealTakenEmail() throws Exception {
        register(email);

        assertThat(register(email)).isEqualTo(202);
        assertThat(userRepository.findAll().stream().filter(u -> email.equalsIgnoreCase(u.getEmail())).count())
                .isEqualTo(1);
    }

    @Test
    @DisplayName("should let the confirmed user log in and read the own profile")
    void shouldLetConfirmedUserReadOwnProfile() throws Exception {
        register(email);
        confirmEmail(email);

        String accessToken = login(email, "senha-forte");

        assertThat(accessToken).isNotBlank();
        assertThat(profileStatus(accessToken)).isEqualTo(200);
    }

    @Test
    @DisplayName("should revoke the previous token after the password change and accept only the new one")
    void shouldRevokePreviousTokenAfterPasswordChange() throws Exception {
        register(email);
        confirmEmail(email);
        String oldToken = login(email, "senha-forte");

        String body = mockMvc.perform(post("/api/me/password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + oldToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"senha-forte\",\"newPassword\":\"senha-nova-forte\"}"))
                .andReturn().getResponse().getContentAsString();
        String newToken = objectMapper.readTree(body).get("accessToken").asString();

        assertThat(profileStatus(oldToken)).isEqualTo(401);
        assertThat(profileStatus(newToken)).isEqualTo(200);
        assertThat(loginStatus(email, "senha-forte")).isEqualTo(401);
        assertThat(loginStatus(email, "senha-nova-forte")).isEqualTo(200);
    }

    @Test
    @DisplayName("should refuse the password change when the current password does not match")
    void shouldRefusePasswordChangeWithWrongCurrentPassword() throws Exception {
        register(email);
        confirmEmail(email);
        String accessToken = login(email, "senha-forte");

        int status = mockMvc.perform(post("/api/me/password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"senha-errada\",\"newPassword\":\"senha-nova-forte\"}"))
                .andReturn().getResponse().getStatus();

        assertThat(status).isEqualTo(401);
        assertThat(profileStatus(accessToken)).isEqualTo(200);
    }

    @Test
    @DisplayName("should invalidate the token and remove the row after the account deletion")
    void shouldInvalidateTokenAfterAccountDeletion() throws Exception {
        register(email);
        confirmEmail(email);
        String accessToken = login(email, "senha-forte");

        int status = mockMvc.perform(delete("/api/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"senha-forte\"}"))
                .andReturn().getResponse().getStatus();

        assertThat(status).isEqualTo(204);
        assertThat(userRepository.findByEmailIgnoreCase(email)).isEmpty();
        assertThat(profileStatus(accessToken)).isEqualTo(401);
    }

    @Test
    @DisplayName("should revoke every session on logout")
    void shouldRevokeEverySessionOnLogout() throws Exception {
        register(email);
        confirmEmail(email);
        String accessToken = login(email, "senha-forte");

        int status = mockMvc.perform(post("/api/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andReturn().getResponse().getStatus();

        assertThat(status).isEqualTo(204);
        assertThat(profileStatus(accessToken)).isEqualTo(401);
    }

    @Test
    @DisplayName("should answer the same on forgot password for a known and an unknown email")
    void shouldAnswerTheSameOnForgotPassword() throws Exception {
        register(email);
        confirmEmail(email);

        int known = mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\"}".formatted(email)))
                .andReturn().getResponse().getStatus();
        int unknown = mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ninguem" + System.nanoTime() + "@example.com\"}"))
                .andReturn().getResponse().getStatus();

        assertThat(known).isEqualTo(200);
        assertThat(unknown).isEqualTo(known);
    }
}
