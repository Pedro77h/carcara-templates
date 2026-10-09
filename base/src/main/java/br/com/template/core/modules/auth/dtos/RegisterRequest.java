package br.com.template.core.modules.auth.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 255) String fullName,
        @Pattern(regexp = "\\d{10,11}") String phone,
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8) String password
) {
}
