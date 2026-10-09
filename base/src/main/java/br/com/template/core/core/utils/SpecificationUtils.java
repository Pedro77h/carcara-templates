package br.com.template.core.core.utils;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

public final class SpecificationUtils {

    private SpecificationUtils() {
    }

    public static <T, V> Specification<T> equalIfPresent(String field, V value) {
        return Objects.isNull(value)
                ? Specification.unrestricted()
                : (root, query, criteriaBuilder) -> criteriaBuilder.equal(path(root, field), value);
    }

    public static <T> Specification<T> likeAnyIfPresent(String term, String... fields) {
        if (Objects.isNull(term) || term.isBlank()) {
            return Specification.unrestricted();
        }

        String likeTerm = "%" + term.strip().toLowerCase(Locale.ROOT) + "%";

        return (root, query, criteriaBuilder) -> {
            Predicate[] predicates = Arrays.stream(fields)
                    .map(field -> criteriaBuilder.like(criteriaBuilder.lower(path(root, field)), likeTerm))
                    .toArray(Predicate[]::new);

            return criteriaBuilder.or(predicates);
        };
    }

    @SuppressWarnings("unchecked")
    private static <T, V> Path<V> path(Root<T> root, String field) {
        Path<?> path = root;

        for (String part : field.split("\\.")) {
            path = path.get(part);
        }

        return (Path<V>) path;
    }
}
