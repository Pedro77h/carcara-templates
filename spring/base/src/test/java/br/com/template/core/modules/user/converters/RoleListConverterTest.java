package br.com.template.core.modules.user.converters;

import br.com.template.core.modules.user.models.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoleListConverterTest {

    private final RoleListConverter converter = new RoleListConverter();

    @Nested
    @DisplayName("convertToDatabaseColumn")
    class ConvertToDatabaseColumn {

        @Test
        @DisplayName("should join the role names inside curly braces")
        void shouldJoinRoleNames() {
            String result = converter.convertToDatabaseColumn(List.of(Role.ROLE_USER, Role.ROLE_ADMIN));

            assertThat(result).isEqualTo("{ROLE_USER,ROLE_ADMIN}");
        }

        @Test
        @DisplayName("should return empty braces when the list is null")
        void shouldReturnEmptyBraces_whenNull() {
            assertThat(converter.convertToDatabaseColumn(null)).isEqualTo("{}");
        }

        @Test
        @DisplayName("should return empty braces when the list is empty")
        void shouldReturnEmptyBraces_whenEmpty() {
            assertThat(converter.convertToDatabaseColumn(List.of())).isEqualTo("{}");
        }
    }

    @Nested
    @DisplayName("convertToEntityAttribute")
    class ConvertToEntityAttribute {

        @Test
        @DisplayName("should parse the stored role names")
        void shouldParseStoredRoleNames() {
            List<Role> result = converter.convertToEntityAttribute("{ROLE_USER,ROLE_ADMIN}");

            assertThat(result).containsExactly(Role.ROLE_USER, Role.ROLE_ADMIN);
        }

        @Test
        @DisplayName("should return an empty list when the column is null")
        void shouldReturnEmptyList_whenNull() {
            assertThat(converter.convertToEntityAttribute(null)).isEmpty();
        }

        @Test
        @DisplayName("should return an empty list when the column holds empty braces")
        void shouldReturnEmptyList_whenEmptyBraces() {
            assertThat(converter.convertToEntityAttribute("{}")).isEmpty();
        }
    }
}
