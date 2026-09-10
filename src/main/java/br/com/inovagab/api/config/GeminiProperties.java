package br.com.inovagab.api.config;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ia.gemini")
public record GeminiProperties(String apiKey, String model, URI baseUrl, int timeoutSeconds) {
}
