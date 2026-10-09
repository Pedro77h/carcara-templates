package br.com.template.core.config.security;

import br.com.template.core.support.CoreTestServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest extends CoreTestServer {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("should never answer forbidden for lack of a csrf token on a state changing request")
    void shouldNeverAnswerForbiddenForLackOfCsrfToken() throws Exception {
        int status = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@example.com\",\"password\":\"senha-errada\"}"))
                .andReturn().getResponse().getStatus();

        assertThat(status).isEqualTo(401);
    }

    @Test
    @DisplayName("should keep the ping endpoint public")
    void shouldKeepPingPublic() throws Exception {
        mockMvc.perform(get("/api/ping"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(200));
    }

    @Test
    @DisplayName("should require authentication on logout even though the rest of /api/auth is public")
    void shouldRequireAuthenticationOnLogout() throws Exception {
        int status = mockMvc.perform(post("/api/auth/logout"))
                .andReturn().getResponse().getStatus();

        assertThat(status).isEqualTo(401);
    }

    @Test
    @DisplayName("should require authentication on the account endpoints")
    void shouldRequireAuthenticationOnAccountEndpoints() throws Exception {
        int status = mockMvc.perform(get("/api/me"))
                .andReturn().getResponse().getStatus();

        assertThat(status).isEqualTo(401);
    }
}
