package br.com.template.core.modules.health.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/ping")
@Tag(name = "Health", description = "Verificação de disponibilidade do serviço")
public class PingController {

    @GetMapping
    @Operation(summary = "Ping", description = "Retorna 200 quando o backend está no ar")
    public ResponseEntity<Map<String, Object>> ping() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "core",
                "timestamp", Instant.now().toString()
        ));
    }
}
