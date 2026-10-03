package dev.viktorstrohush.orderflow.order.infrastructure.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;

/**
 * Emisor de tokens SOLO para la demo local. En un entorno real los tokens los emitiría un
 * Identity Provider (Keycloak, Auth0...) y los servicios solo actuarían como resource servers.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth (demo)")
@ConditionalOnProperty(name = "orderflow.auth.demo-login-enabled", havingValue = "true")
class AuthController {

    private static final Duration TTL = Duration.ofHours(8);

    private final JwtEncoder encoder;

    AuthController(JwtEncoder encoder) {
        this.encoder = encoder;
    }

    @PostMapping("/token")
    @Operation(summary = "Devuelve un JWT de demo para el usuario indicado")
    TokenResponse token(@Valid @RequestBody TokenRequest request) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("orderflow-demo")
                .subject(request.username())
                .issuedAt(now)
                .expiresAt(now.plus(TTL))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponse(token, TTL.toSeconds());
    }

    record TokenRequest(@NotBlank @Pattern(regexp = "[a-zA-Z0-9._-]{3,30}") String username) {
    }

    record TokenResponse(String accessToken, long expiresIn) {
    }
}
