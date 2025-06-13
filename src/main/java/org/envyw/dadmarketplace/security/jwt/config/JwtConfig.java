package org.envyw.dadmarketplace.security.jwt.config;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
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

    @Bean
    public JwtEncoder jwtEncoder() {
        JWK jwk = generateRsaKey();
        JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(jwk));

        return new NimbusJwtEncoder(jwks);
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        RSAKey rsaKey = (RSAKey) generateRsaKey();

        try {
            RSAPublicKey publicKey = rsaKey.toRSAPublicKey();

            return NimbusJwtDecoder.withPublicKey(publicKey).build();

        } catch (Exception e) {
            log.error("JwtDecoder 생성 실패", e);
            throw new RuntimeException("JwtDecoder 생성에 실패했습니다", e);
        }
    }

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        RSAKey rsaKey = (RSAKey) generateRsaKey();

        try {
            RSAPublicKey publicKey = rsaKey.toRSAPublicKey();

            return NimbusReactiveJwtDecoder.withPublicKey(publicKey).build();

        } catch (Exception e) {
            log.error("ReactiveJwtDecoder 생성 실패", e);
            throw new RuntimeException("ReactiveJwtDecoder 생성에 실패했습니다", e);
        }
    }


    private JWK generateRsaKey() {
        try {

            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

            return new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(keyId)
                    .build();

        } catch (NoSuchAlgorithmException e) {
            log.error("RSA 키 생성 실패", e);
            throw new RuntimeException("RSA 키 생성에 실패했습니다", e);
        }
    }
}
