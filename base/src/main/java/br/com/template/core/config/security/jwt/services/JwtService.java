package br.com.template.core.config.security.jwt.services;

import br.com.template.core.config.security.jwt.models.JwtProperties;
import br.com.template.core.modules.user.models.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class JwtService {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_TOKEN_TYPE = "tokenType";
    private static final String CLAIM_SESSION_VERSION = "sessionVersion";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private final SecretKey signingKey;
    private final String issuer;
    private final long accessTokenExpirationMinutes;
    private final long refreshTokenExpirationDays;

    public JwtService(JwtProperties jwtProperties) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secret()));
        this.issuer = jwtProperties.issuer();
        this.accessTokenExpirationMinutes = jwtProperties.accessTokenExpirationMinutes();
        this.refreshTokenExpirationDays = jwtProperties.refreshTokenExpirationDays();
    }

    public String generateAccessToken(User user) {
        return buildToken(user, TOKEN_TYPE_ACCESS, accessTokenExpirationMinutes, ChronoUnit.MINUTES);
    }

    public String generateRefreshToken(User user) {
        return buildToken(user, TOKEN_TYPE_REFRESH, refreshTokenExpirationDays, ChronoUnit.DAYS);
    }

    public TokenClaims parseAccessToken(String token) {
        return parseTokenOfType(token, TOKEN_TYPE_ACCESS);
    }

    public TokenClaims parseRefreshToken(String token) {
        return parseTokenOfType(token, TOKEN_TYPE_REFRESH);
    }

    public TokenClaims parseToken(String token) {
        Claims claims = parseClaims(token);
        Number claimedUserId = claims.get(CLAIM_USER_ID, Number.class);

        if (Objects.isNull(claimedUserId)) {
            throw new JwtException("Token without userId claim");
        }

        Number claimedSessionVersion = claims.get(CLAIM_SESSION_VERSION, Number.class);
        List<String> claimedRoles = claims.get(CLAIM_ROLES, List.class);

        return new TokenClaims(
                claimedUserId.longValue(),
                claims.getSubject(),
                Objects.isNull(claimedRoles) ? Collections.emptyList() : claimedRoles,
                claims.get(CLAIM_TOKEN_TYPE, String.class),
                claims.getIssuedAt().toInstant(),
                Objects.isNull(claimedSessionVersion) ? 0 : claimedSessionVersion.intValue()
        );
    }

    private TokenClaims parseTokenOfType(String token, String expectedTokenType) {
        TokenClaims tokenClaims = parseToken(token);

        if (!expectedTokenType.equals(tokenClaims.tokenType())) {
            throw new IllegalArgumentException("Invalid " + expectedTokenType + " token type");
        }

        return tokenClaims;
    }

    private String buildToken(User user, String tokenType, long expirationAmount, ChronoUnit unit) {
        Instant now = Instant.now();

        return Jwts.builder()
                .issuer(issuer)
                .subject(user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationAmount, unit)))
                .claim(CLAIM_USER_ID, user.getId())
                .claim(CLAIM_ROLES, collectRoleNames(user))
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .claim(CLAIM_SESSION_VERSION, user.getSessionVersion())
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private List<String> collectRoleNames(User user) {
        return Stream.concat(
                        Optional.ofNullable(user.getRoles()).orElse(List.of()).stream(),
                        Optional.ofNullable(user.getPrincipalRole()).stream()
                )
                .filter(Objects::nonNull)
                .map(Enum::name)
                .distinct()
                .toList();
    }

    public record TokenClaims(Long userId, String subject, List<String> roles, String tokenType, Instant issuedAt,
                              int sessionVersion) {
    }
}
