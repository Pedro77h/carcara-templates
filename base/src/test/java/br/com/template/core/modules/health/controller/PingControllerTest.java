package br.com.template.core.modules.health.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PingControllerTest {

    private final PingController pingController = new PingController();

    @Test
    @DisplayName("should answer 200 with the service status")
    void shouldAnswerWithServiceStatus() {
        ResponseEntity<Map<String, Object>> response = pingController.ping();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "UP");
        assertThat(response.getBody()).containsEntry("service", "core");
        assertThat(response.getBody()).containsKey("timestamp");
    }
}
