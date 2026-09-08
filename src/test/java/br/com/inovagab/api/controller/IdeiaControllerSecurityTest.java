package br.com.inovagab.api.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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
import br.com.inovagab.api.dto.CriarIdeiaRequest;
import br.com.inovagab.api.dto.IdeiaResponse;
import br.com.inovagab.api.dto.PaginaResponse;
import br.com.inovagab.api.exception.GlobalExceptionHandler;
import br.com.inovagab.api.model.Role;
import br.com.inovagab.api.model.StatusIdeia;
import br.com.inovagab.api.security.SecurityErrorHandler;
import br.com.inovagab.api.service.IdeiaService;

@WebMvcTest(IdeiaController.class)
@Import({ SecurityConfig.class, JwtConfig.class, SecurityErrorHandler.class, GlobalExceptionHandler.class })
@TestPropertySource(properties = {
		"app.jwt.secret=c2VncmVkby1kZS10ZXN0ZS1jb20tbWFpcy1kZS0yNTYtYml0cw==",
		"app.jwt.expiration-seconds=3600", "app.jwt.issuer=inovagab-api"
})
class IdeiaControllerSecurityTest {

	private static final String BODY = """
			{"titulo":" Ideia válida ","problema":" Problema operacional relevante ",
			"solucaoProposta":" Solução proposta detalhada ",
			"beneficiosEsperados":" Benefícios esperados relevantes ",
			"categoria":" Inovação ","estrategiaId":" estrategia-id "}
			""";

	@Autowired private MockMvc mockMvc;
	@Autowired private JwtEncoder jwtEncoder;
	@MockitoBean private IdeiaService ideiaService;

	@Test
	void operadorPodeCriarComValoresNormalizadosESemDadosSensiveis() throws Exception {
		when(ideiaService.criar(any(), eq("usuario-id"))).thenReturn(response());
		mockMvc.perform(post("/api/ideias").header("Authorization", bearer(Role.OPERADOR))
				.contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("ENVIADA"))
				.andExpect(content().string(not(containsString("senhaHash"))))
				.andExpect(content().string(not(containsString("token"))));
		ArgumentCaptor<CriarIdeiaRequest> captor = ArgumentCaptor.forClass(CriarIdeiaRequest.class);
		org.mockito.Mockito.verify(ideiaService).criar(captor.capture(), eq("usuario-id"));
		org.assertj.core.api.Assertions.assertThat(captor.getValue().titulo()).isEqualTo("Ideia válida");
		org.assertj.core.api.Assertions.assertThat(captor.getValue().estrategiaId()).isEqualTo("estrategia-id");
	}

	@ParameterizedTest
	@EnumSource(value = Role.class, names = { "GESTOR", "LIDERANCA" })
	void gestorELiderancaNaoPodemCriar(Role role) throws Exception {
		mockMvc.perform(post("/api/ideias").header("Authorization", bearer(role))
				.contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());
	}

	@Test
	void operadorAcessaMinhasMasNaoListagemGeral() throws Exception {
		when(ideiaService.listarMinhas(anyInt(), anyInt(), eq("usuario-id")))
				.thenReturn(PaginaResponse.de(List.of(response()), 0, 20, 1));
		String auth = bearer(Role.OPERADOR);
		mockMvc.perform(get("/api/ideias/minhas").header("Authorization", auth)).andExpect(status().isOk());
		mockMvc.perform(get("/api/ideias").header("Authorization", auth)).andExpect(status().isForbidden());
	}

	@ParameterizedTest
	@EnumSource(value = Role.class, names = { "GESTOR", "LIDERANCA" })
	void gestorELiderancaConsultamListagem(Role role) throws Exception {
		when(ideiaService.listar(any(), any(), any(), any(), anyInt(), anyInt(), eq("usuario-id")))
				.thenReturn(PaginaResponse.de(List.of(response()), 0, 20, 1));
		mockMvc.perform(get("/api/ideias").header("Authorization", bearer(role))).andExpect(status().isOk());
	}

	@Test
	void somenteGestorExecutaAvaliacao() throws Exception {
		when(ideiaService.analisar("ideia-id", "usuario-id")).thenReturn(response());
		mockMvc.perform(patch("/api/ideias/ideia-id/analisar").header("Authorization", bearer(Role.GESTOR)))
				.andExpect(status().isOk());
		mockMvc.perform(patch("/api/ideias/ideia-id/analisar").header("Authorization", bearer(Role.LIDERANCA)))
				.andExpect(status().isForbidden());
		mockMvc.perform(patch("/api/ideias/ideia-id/analisar").header("Authorization", bearer(Role.OPERADOR)))
				.andExpect(status().isForbidden());
	}

	@Test
	void semJwtRetornaNaoAutorizado() throws Exception {
		mockMvc.perform(get("/api/ideias/minhas")).andExpect(status().isUnauthorized());
	}

	@Test
	void textosInvalidosDepoisDeStripRetornamBadRequest() throws Exception {
		mockMvc.perform(post("/api/ideias").header("Authorization", bearer(Role.OPERADOR))
				.contentType(MediaType.APPLICATION_JSON).content(BODY.replace(" Ideia válida ", " a ")))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
	}

	private String bearer(Role role) {
		Instant agora = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder().issuer("inovagab-api").issuedAt(agora)
				.expiresAt(agora.plusSeconds(3600)).subject("usuario-id").claim("role", role.name()).build();
		return "Bearer " + jwtEncoder.encode(JwtEncoderParameters.from(
				JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
	}

	private IdeiaResponse response() {
		return new IdeiaResponse("ideia-id", "Ideia válida", "Problema operacional relevante",
				"Solução proposta detalhada", "Benefícios esperados relevantes", "Inovação", "estrategia-id",
				"usuario-id", StatusIdeia.ENVIADA, null, null, null, null, Instant.now(), Instant.now(), 0L);
	}
}
