package br.com.template.core.modules.user.converters;

import br.com.template.core.modules.user.models.Role;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Converter
public class RoleListConverter implements AttributeConverter<List<Role>, String> {

    private static final String EMPTY_ROLES = "{}";

    @Override
    public String convertToDatabaseColumn(List<Role> roles) {
        if (Objects.isNull(roles) || roles.isEmpty()) {
            return EMPTY_ROLES;
        }

        return roles.stream()
                .map(Role::name)
                .collect(Collectors.joining(",", "{", "}"));
    }

    @Override
    public List<Role> convertToEntityAttribute(String dbData) {
        if (Objects.isNull(dbData) || dbData.equals(EMPTY_ROLES)) {
            return List.of();
        }

        String content = dbData
                .replace("{", "")
                .replace("}", "");

        return Arrays.stream(content.split(","))
                .map(Role::valueOf)
                .toList();
    }
}
