package org.envyw.dadmarketplace.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.*;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Configuration
@Slf4j
public class JwtConfig {

    @Value("${app.jwt.key-id:dad-marketplace-key}")
    private String keyId;

    private RSAKey rsaKey;
    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;

    @PostConstruct
    public void init() {
        generateKeyPair();
        log.info("RSA JWT 키 초기화 완료: keyId={}", keyId);
    }

    private void generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            this.publicKey = (RSAPublicKey) keyPair.getPublic();
            this.privateKey = (RSAPrivateKey) keyPair.getPrivate();

            this.rsaKey = new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(keyId)
                    .build();

        } catch (NoSuchAlgorithmException e) {
            log.error("RSA 키 생성 실패", e);
            throw new RuntimeException("RSA 키 생성에 실패했습니다", e);
        }
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(rsaKey));
        return new NimbusJwtEncoder(jwks);
    }

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        try {
            return NimbusReactiveJwtDecoder.withPublicKey(publicKey).build();
        } catch (Exception e) {
            log.error("ReactiveJwtDecoder 생성 실패", e);
            throw new RuntimeException("ReactiveJwtDecoder 생성에 실패했습니다", e);
        }
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        try {
            return NimbusJwtDecoder.withPublicKey(publicKey).build();

        } catch (Exception e) {
            log.error("JwtDecoder 생성 실패", e);
            throw new RuntimeException("JwtDecoder 생성에 실패했습니다", e);
        }
    }
}
