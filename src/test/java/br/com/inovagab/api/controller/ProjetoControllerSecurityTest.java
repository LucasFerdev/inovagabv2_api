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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
import br.com.inovagab.api.dto.PaginaResponse;
import br.com.inovagab.api.dto.ProjetoResponse;
import br.com.inovagab.api.exception.GlobalExceptionHandler;
import br.com.inovagab.api.exception.OperacaoProjetoInvalidaException;
import br.com.inovagab.api.exception.ProjetoNaoEncontradoException;
import br.com.inovagab.api.model.EtapaProjeto;
import br.com.inovagab.api.model.Role;
import br.com.inovagab.api.model.StatusProjeto;
import br.com.inovagab.api.security.SecurityErrorHandler;
import br.com.inovagab.api.service.ProjetoService;

@WebMvcTest(ProjetoController.class)
@Import({ SecurityConfig.class, JwtConfig.class, SecurityErrorHandler.class, GlobalExceptionHandler.class })
@TestPropertySource(properties = { "app.jwt.secret=c2VncmVkby1kZS10ZXN0ZS1jb20tbWFpcy1kZS0yNTYtYml0cw==",
		"app.jwt.expiration-seconds=3600", "app.jwt.issuer=inovagab-api" })
class ProjetoControllerSecurityTest {

	private static final String BODY = """
			{"nome":"Projeto válido","descricao":"Descrição completa do projeto","estrategiaId":"estrategia-id",
			"investimento":1000.00,"prazo":"2027-10-01"}
			""";
	@Autowired private MockMvc mockMvc;
	@Autowired private JwtEncoder jwtEncoder;
	@MockitoBean private ProjetoService projetoService;

	@Test
	void gestorPodeCriarEConsultarSemDadosSensiveis() throws Exception {
		when(projetoService.criar(any(), eq("usuario-id"))).thenReturn(response());
		when(projetoService.listar(any(), any(), any(), any(), any(), anyInt(), anyInt(), eq("usuario-id")))
				.thenReturn(PaginaResponse.de(List.of(response()), 0, 20, 1));
		String auth = bearer(Role.GESTOR);
		mockMvc.perform(post("/api/projetos").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isCreated()).andExpect(content().string(not(containsString("senhaHash"))));
		mockMvc.perform(get("/api/projetos").header("Authorization", auth)).andExpect(status().isOk());
	}

	@Test
	void liderancaConsultaMasNaoEscreve() throws Exception {
		when(projetoService.listar(any(), any(), any(), any(), any(), anyInt(), anyInt(), eq("usuario-id")))
				.thenReturn(PaginaResponse.de(List.of(response()), 0, 20, 1));
		String auth = bearer(Role.LIDERANCA);
		mockMvc.perform(get("/api/projetos").header("Authorization", auth)).andExpect(status().isOk());
		mockMvc.perform(post("/api/projetos").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isForbidden());
	}

	@ParameterizedTest
	@EnumSource(value = Role.class, names = { "OPERADOR" })
	void operadorNaoAcessaProjetos(Role role) throws Exception {
		String auth = bearer(role);
		mockMvc.perform(get("/api/projetos").header("Authorization", auth)).andExpect(status().isForbidden());
		mockMvc.perform(post("/api/projetos").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isForbidden());
	}

	@Test
	void semTokenRetorna401EMetodoIncorretoRetorna405() throws Exception {
		mockMvc.perform(get("/api/projetos")).andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/projetos/projeto-id").header("Authorization", bearer(Role.GESTOR)))
				.andExpect(status().isMethodNotAllowed()).andExpect(jsonPath("$.status").value(405));
	}

	@Test
	void dadosInvalidosRetornam400() throws Exception {
		mockMvc.perform(post("/api/projetos").header("Authorization", bearer(Role.GESTOR))
				.contentType(MediaType.APPLICATION_JSON).content(BODY.replace("Projeto válido", " a ")))
				.andExpect(status().isBadRequest());
	}

	@Test
	void projetoInexistenteRetorna404() throws Exception {
		when(projetoService.buscarPorId("inexistente", "usuario-id")).thenThrow(new ProjetoNaoEncontradoException());
		mockMvc.perform(get("/api/projetos/inexistente").header("Authorization", bearer(Role.GESTOR)))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void operacaoInvalidaRetorna409() throws Exception {
		when(projetoService.concluir("projeto-id", "usuario-id"))
				.thenThrow(new OperacaoProjetoInvalidaException("Projeto concluído não pode ser alterado"));
		mockMvc.perform(patch("/api/projetos/projeto-id/concluir").header("Authorization", bearer(Role.GESTOR)))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
	}

	private String bearer(Role role) {
		Instant agora = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder().issuer("inovagab-api").issuedAt(agora).expiresAt(agora.plusSeconds(3600))
				.subject("usuario-id").claim("role", role.name()).build();
		return "Bearer " + jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
				.getTokenValue();
	}

	private ProjetoResponse response() {
		return new ProjetoResponse("projeto-id", "Projeto válido", "Descrição completa do projeto", "estrategia-id", null,
				EtapaProjeto.PLANEJAMENTO, StatusProjeto.PLANEJADO, 0, new BigDecimal("1000.00"), LocalDate.of(2027, 10, 1),
				BigDecimal.ZERO, BigDecimal.ZERO, null, "usuario-id", Instant.now(), Instant.now(), 0L);
	}
}
