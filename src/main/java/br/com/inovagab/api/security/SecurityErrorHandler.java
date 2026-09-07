package br.com.inovagab.api.security;

import java.io.IOException;
import java.time.Instant;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import br.com.inovagab.api.dto.RespostaErro;
import tools.jackson.databind.ObjectMapper;

@Component
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

	private final ObjectMapper objectMapper;

	public SecurityErrorHandler(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authenticationException) throws IOException {
		escrever(response, request, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized", "Token ausente ou inválido");
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException, ServletException {
		escrever(response, request, HttpServletResponse.SC_FORBIDDEN, "Forbidden", "Acesso negado");
	}

	private void escrever(HttpServletResponse response, HttpServletRequest request, int status, String erro,
			String mensagem) throws IOException {
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		RespostaErro resposta = new RespostaErro(Instant.now(), status, erro, mensagem, request.getRequestURI());
		objectMapper.writeValue(response.getOutputStream(), resposta);
	}
}
