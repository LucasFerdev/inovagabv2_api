package br.com.inovagab.api.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cadastro")
public record CadastroProperties(String codigoGestor, String codigoLideranca) {

	public CadastroProperties {
		codigoGestor = normalizar(codigoGestor);
		codigoLideranca = normalizar(codigoLideranca);
		if (!codigoGestor.isEmpty() && comparacaoSegura(codigoGestor, codigoLideranca)) {
			throw new IllegalArgumentException("Os códigos de acesso de Gestor e Liderança devem ser diferentes");
		}
	}

	private static String normalizar(String codigo) {
		return codigo == null ? "" : codigo.strip();
	}

	private static boolean comparacaoSegura(String primeiro, String segundo) {
		return MessageDigest.isEqual(
				primeiro.getBytes(StandardCharsets.UTF_8),
				segundo.getBytes(StandardCharsets.UTF_8));
	}
}
