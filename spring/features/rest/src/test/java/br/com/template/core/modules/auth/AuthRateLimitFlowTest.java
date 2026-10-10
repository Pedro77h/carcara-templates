package br.com.template.core.modules.auth;

import br.com.template.core.modules.auth.repositories.AuthAttemptRepository;
import br.com.template.core.modules.user.models.User;
import br.com.template.core.modules.user.repositories.UserRepository;
import br.com.template.core.support.CoreTestServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
class AuthRateLimitFlowTest extends CoreTestServer {

    private static final int MAX_ATTEMPTS_PER_EMAIL = 5;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthAttemptRepository authAttemptRepository;

    private String email;

    @BeforeEach
    void setUp() throws Exception {
        authAttemptRepository.deleteAll();
        email = "ana" + System.nanoTime() + "@example.com";

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "fullName": "Ana Souza",
                          "phone": "85999990000",
                          "email": "%s",
                          "password": "senha-forte"
                        }
                        """.formatted(email)));

        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        user.setEmailVerifiedAt(OffsetDateTime.now());
        userRepository.save(user);
    }

    private int attemptLogin(String email, String password, String remoteAddress) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .with(request -> {
                            request.setRemoteAddr(remoteAddress);
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andReturn().getResponse().getStatus();
    }

    @Test
    @DisplayName("should answer 429 once the failed attempts for the email reach the limit")
    void shouldAnswerTooManyRequestsAfterTheLimit() throws Exception {
        for (int attempt = 0; attempt < MAX_ATTEMPTS_PER_EMAIL; attempt++) {
            assertThat(attemptLogin(email, "senha-errada", "187.19.20." + attempt)).isEqualTo(401);
        }

        assertThat(attemptLogin(email, "senha-errada", "187.19.20.99")).isEqualTo(429);
        assertThat(attemptLogin(email, "senha-forte", "187.19.20.99")).isEqualTo(429);
    }

    @Test
    @DisplayName("should clear the counter after a successful login")
    void shouldClearTheCounterAfterSuccess() throws Exception {
        assertThat(attemptLogin(email, "senha-errada", "187.19.20.1")).isEqualTo(401);
        assertThat(attemptLogin(email, "senha-forte", "187.19.20.1")).isEqualTo(200);

        for (int attempt = 0; attempt < MAX_ATTEMPTS_PER_EMAIL - 1; attempt++) {
            assertThat(attemptLogin(email, "senha-errada", "187.19.20." + attempt)).isEqualTo(401);
        }

        assertThat(attemptLogin(email, "senha-forte", "187.19.20.50")).isEqualTo(200);
    }

    @Test
    @DisplayName("should never store the email in clear text among the attempts")
    void shouldNeverStoreTheEmailInClearText() throws Exception {
        attemptLogin(email, "senha-errada", "187.19.20.1");

        assertThat(authAttemptRepository.findAll())
                .isNotEmpty()
                .noneMatch(attempt -> attempt.getIdentifier().contains(email))
                .noneMatch(attempt -> attempt.getIdentifier().contains("187.19.20.1"));
    }
}
