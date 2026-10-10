package br.com.template.core.modules.user.dtos;

import br.com.template.core.modules.user.models.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeDto {

    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private Role principalRole;
    private boolean emailVerified;
    private OffsetDateTime createdAt;
}
