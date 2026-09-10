package br.com.inovagab.api.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.inovagab.api.config.JwtConfig;
import br.com.inovagab.api.config.SecurityConfig;
import br.com.inovagab.api.dto.AnaliseIaIdeiaResponse;
import br.com.inovagab.api.exception.GeminiIndisponivelException;
import br.com.inovagab.api.exception.GeminiLimiteExcedidoException;
import br.com.inovagab.api.exception.GeminiNaoConfiguradoException;
import br.com.inovagab.api.exception.GeminiTimeoutException;
import br.com.inovagab.api.exception.GlobalExceptionHandler;
import br.com.inovagab.api.model.AnaliseIaIdeia;
import br.com.inovagab.api.model.Role;
import br.com.inovagab.api.security.SecurityErrorHandler;
import br.com.inovagab.api.service.AnaliseIaService;

@WebMvcTest(AnaliseIaController.class)
@Import({ SecurityConfig.class, JwtConfig.class, SecurityErrorHandler.class, GlobalExceptionHandler.class })
@TestPropertySource(properties = {
		"app.jwt.secret=c2VncmVkby1kZS10ZXN0ZS1jb20tbWFpcy1kZS0yNTYtYml0cw==",
		"app.jwt.expiration-seconds=3600", "app.jwt.issuer=inovagab-api"
})
class AnaliseIaControllerSecurityTest {

	@Autowired private MockMvc mockMvc;
	@Autowired private JwtEncoder jwtEncoder;
	@MockitoBean private AnaliseIaService analiseIaService;

	@Test
	void gestorExecutaAnalise() throws Exception {
		when(analiseIaService.analisar("ideia-id", false, "usuario-id")).thenReturn(response());
		mockMvc.perform(post("/api/ia/ideias/ideia-id/analisar").header("Authorization", bearer(Role.GESTOR)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.pontuacaoGeral").value(85))
				.andExpect(content().string(not(containsString("senhaHash"))))
				.andExpect(content().string(not(containsString("token"))));
	}

	@ParameterizedTest
	@EnumSource(value = Role.class, names = { "OPERADOR", "LIDERANCA" })
	void operadorELiderancaRecebem403NoPost(Role role) throws Exception {
		mockMvc.perform(post("/api/ia/ideias/ideia-id/analisar").header("Authorization", bearer(role)))
				.andExpect(status().isForbidden());
	}

	@ParameterizedTest
	@EnumSource(value = Role.class, names = { "GESTOR", "LIDERANCA" })
	void gestorELiderancaConsultamAnaliseSalva(Role role) throws Exception {
		when(analiseIaService.consultar("ideia-id", "usuario-id")).thenReturn(response());
		mockMvc.perform(get("/api/ia/ideias/ideia-id/analise").header("Authorization", bearer(role)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.aviso").value(AnaliseIaIdeia.AVISO_PADRAO));
	}

	@Test
	void semTokenRetorna401() throws Exception {
		mockMvc.perform(post("/api/ia/ideias/ideia-id/analisar")).andExpect(status().isUnauthorized());
	}

	@Test
	void falhasExternasConhecidasNaoRetornam500() throws Exception {
		String auth = bearer(Role.GESTOR);
		when(analiseIaService.analisar("sem-chave", false, "usuario-id"))
				.thenThrow(new GeminiNaoConfiguradoException());
		when(analiseIaService.analisar("limite", false, "usuario-id"))
				.thenThrow(new GeminiLimiteExcedidoException());
		when(analiseIaService.analisar("timeout", false, "usuario-id"))
				.thenThrow(new GeminiTimeoutException());
		when(analiseIaService.analisar("indisponivel", false, "usuario-id"))
				.thenThrow(new GeminiIndisponivelException());
		mockMvc.perform(post("/api/ia/ideias/sem-chave/analisar").header("Authorization", auth))
				.andExpect(status().isServiceUnavailable());
		mockMvc.perform(post("/api/ia/ideias/limite/analisar").header("Authorization", auth))
				.andExpect(status().isTooManyRequests());
		mockMvc.perform(post("/api/ia/ideias/timeout/analisar").header("Authorization", auth))
				.andExpect(status().isGatewayTimeout());
		mockMvc.perform(post("/api/ia/ideias/indisponivel/analisar").header("Authorization", auth))
				.andExpect(status().isBadGateway());
	}

	private String bearer(Role role) {
		Instant agora = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder().issuer("inovagab-api").issuedAt(agora)
				.expiresAt(agora.plusSeconds(3600)).subject("usuario-id").claim("role", role.name()).build();
		return "Bearer " + jwtEncoder.encode(JwtEncoderParameters.from(
				JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
	}

	private AnaliseIaIdeiaResponse response() {
		return new AnaliseIaIdeiaResponse("ideia-id", 85, 4, "Resumo consultivo", List.of("Impacto"),
				List.of("Adoção"), List.of("Piloto"), "gemini-3.5-flash-lite", Instant.now(),
				AnaliseIaIdeia.AVISO_PADRAO);
	}
}
