package br.com.template.core.core.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PseudonymHasherTest {

    private static final String SECRET = "segredo-de-pseudonimo-para-teste";
    private static final String OTHER_SECRET = "outro-segredo-de-pseudonimo";

    @Test
    @DisplayName("should build the same pseudonym for the same value and secret")
    void shouldBeStableForTheSameSecret() {
        PseudonymHasher hasher = new PseudonymHasher(SECRET, new MockEnvironment());

        Optional<String> first = hasher.hash("187.19.20.21");
        Optional<String> second = hasher.hash("187.19.20.21");

        assertThat(first).isPresent().isEqualTo(second);
        assertThat(first.orElseThrow()).hasSize(64).doesNotContain("187.19.20.21");
    }

    @Test
    @DisplayName("should build different pseudonyms for different values")
    void shouldDifferForDifferentValues() {
        PseudonymHasher hasher = new PseudonymHasher(SECRET, new MockEnvironment());

        assertThat(hasher.hash("187.19.20.21")).isNotEqualTo(hasher.hash("187.19.20.22"));
    }

    @Test
    @DisplayName("should build different pseudonyms when the secret changes")
    void shouldDifferForDifferentSecrets() {
        PseudonymHasher hasher = new PseudonymHasher(SECRET, new MockEnvironment());
        PseudonymHasher rotatedHasher = new PseudonymHasher(OTHER_SECRET, new MockEnvironment());

        assertThat(hasher.hash("187.19.20.21")).isNotEqualTo(rotatedHasher.hash("187.19.20.21"));
    }

    @Test
    @DisplayName("should return empty for a missing or blank value")
    void shouldReturnEmptyForMissingValue() {
        PseudonymHasher hasher = new PseudonymHasher(SECRET, new MockEnvironment());

        assertThat(hasher.hash(null)).isEmpty();
        assertThat(hasher.hash("   ")).isEmpty();
    }

    @Test
    @DisplayName("should generate an ephemeral secret outside production")
    void shouldGenerateEphemeralSecretOutsideProduction() {
        PseudonymHasher first = new PseudonymHasher("", new MockEnvironment().withProperty("x", "y"));
        PseudonymHasher second = new PseudonymHasher("", new MockEnvironment());

        assertThat(first.hash("187.19.20.21")).isNotEqualTo(second.hash("187.19.20.21"));
    }

    @Test
    @DisplayName("should refuse to start in production without a configured secret")
    void shouldRefuseToStartInProductionWithoutSecret() {
        MockEnvironment productionEnvironment = new MockEnvironment();
        productionEnvironment.setActiveProfiles("prod");

        assertThatThrownBy(() -> new PseudonymHasher("", productionEnvironment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("pseudonym-secret");
    }
}
