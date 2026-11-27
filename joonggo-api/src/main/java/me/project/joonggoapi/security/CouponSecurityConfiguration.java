package me.project.joonggoapi.security;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Configuration
class CouponSecurityConfiguration {

    @Bean
    SecurityFilterChain couponSecurity(HttpSecurity http, ObjectProvider<JwtDecoder> decoder) throws Exception {
        http.securityMatcher("/api/v1/coupon/**", "/api/v2/coupon/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rules -> rules.anyRequest().authenticated())
                .exceptionHandling(errors -> errors.authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint()));
        if (decoder.getIfAvailable() != null) {
            http.oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));
        }
        return http.build();
    }

    @Bean
    @ConditionalOnProperty(name = {"coupon.security.issuer", "coupon.security.audience", "coupon.security.jwks"})
    JwtDecoder couponJwtDecoder(@Value("${coupon.security.issuer}") String issuer,
                                 @Value("${coupon.security.audience}") String audience,
                                 @Value("${coupon.security.jwks}") String jwkSetUri) {
        if (issuer.isBlank() || audience.isBlank() || jwkSetUri.isBlank()) {
            throw new IllegalArgumentException("Coupon JWT issuer, audience and JWK Set URI must be configured");
        }

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                new JwtClaimValidator<Instant>("exp", Objects::nonNull),
                new JwtClaimValidator<List<String>>("aud", values -> values != null && values.contains(audience))));
        return decoder;
    }
}
