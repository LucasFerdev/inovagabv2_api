package br.com.inovagab.api.config;

import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import br.com.inovagab.api.security.JwtProperties;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

	@Bean
	public JwtEncoder jwtEncoder(JwtProperties properties) {
		return NimbusJwtEncoder.withSecretKey(secretKey(properties)).build();
	}

	@Bean
	public JwtDecoder jwtDecoder(JwtProperties properties) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey(properties))
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
		decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
		return decoder;
	}

	private SecretKey secretKey(JwtProperties properties) {
		byte[] secretBytes;
		try {
			secretBytes = Base64.getDecoder().decode(properties.secret());
		} catch (IllegalArgumentException exception) {
			throw new IllegalStateException("JWT_SECRET deve estar em Base64 válido", exception);
		}
		if (secretBytes.length < 32) {
			throw new IllegalStateException("JWT_SECRET deve conter pelo menos 256 bits");
		}
		return new SecretKeySpec(secretBytes, "HmacSHA256");
	}
}
