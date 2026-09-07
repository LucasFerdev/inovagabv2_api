package br.com.inovagab.api.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long expirationSeconds, String issuer) {

	public JwtProperties {
		if (secret == null || secret.isBlank()) {
			throw new IllegalArgumentException("A variável JWT_SECRET deve ser configurada");
		}
		if (expirationSeconds <= 0) {
			throw new IllegalArgumentException("A expiração do JWT deve ser positiva");
		}
		if (issuer == null || issuer.isBlank()) {
			throw new IllegalArgumentException("O emissor do JWT deve ser configurado");
		}
	}
}
