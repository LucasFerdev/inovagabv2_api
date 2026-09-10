package br.com.inovagab.api.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.OptimisticLockingFailureException;
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
import br.com.inovagab.api.dto.AtualizarEstrategiaRequest;
import br.com.inovagab.api.dto.CriarEstrategiaRequest;
import br.com.inovagab.api.dto.EstrategiaResponse;
import br.com.inovagab.api.dto.PaginaResponse;
import br.com.inovagab.api.exception.EstrategiaNaoEncontradaException;
import br.com.inovagab.api.exception.GlobalExceptionHandler;
import br.com.inovagab.api.exception.OperacaoEstrategiaInvalidaException;
import br.com.inovagab.api.exception.UsuarioInativoException;
import br.com.inovagab.api.model.Role;
import br.com.inovagab.api.model.StatusEstrategia;
import br.com.inovagab.api.security.SecurityErrorHandler;
import br.com.inovagab.api.service.EstrategiaService;

@WebMvcTest(EstrategiaController.class)
@Import({ SecurityConfig.class, JwtConfig.class, SecurityErrorHandler.class, GlobalExceptionHandler.class })
@TestPropertySource(properties = {
		"app.jwt.secret=c2VncmVkby1kZS10ZXN0ZS1jb20tbWFpcy1kZS0yNTYtYml0cw==",
		"app.jwt.expiration-seconds=3600",
		"app.jwt.issuer=inovagab-api"
})
class EstrategiaControllerSecurityTest {

	private static final String BODY_VALIDO = """
			{
			  "titulo": "Estratégia 2027",
			  "descricao": "Descrição completa da estratégia",
			  "data": "2027-01-10",
			  "categoria": "Inovação",
			  "campanha": "Campanha anual"
			}
			""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtEncoder jwtEncoder;

	@MockitoBean
	private EstrategiaService estrategiaService;

	@Test
	void liderancaPodeCriarEstrategiaSemExporDadosSensiveis() throws Exception {
		when(estrategiaService.criar(any(), eq("usuario-id"))).thenReturn(estrategiaResponse());

		mockMvc.perform(post("/api/estrategias")
				.header("Authorization", bearer(Role.LIDERANCA))
				.contentType(MediaType.APPLICATION_JSON)
				.content(BODY_VALIDO))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("RASCUNHO"))
				.andExpect(content().string(not(containsString("senhaHash"))))
				.andExpect(content().string(not(containsString("token"))));
	}

	@Test
	void liderancaPodeAtualizarAtivarDesativarEArquivar() throws Exception {
		when(estrategiaService.atualizar(eq("estrategia-id"), any(), eq("usuario-id")))
				.thenReturn(estrategiaResponse());
		when(estrategiaService.ativar("estrategia-id", "usuario-id")).thenReturn(estrategiaResponse());
		when(estrategiaService.desativar("estrategia-id", "usuario-id")).thenReturn(estrategiaResponse());
		String authorization = bearer(Role.LIDERANCA);

		mockMvc.perform(put("/api/estrategias/estrategia-id")
				.header("Authorization", authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content(BODY_VALIDO))
				.andExpect(status().isOk());
		mockMvc.perform(patch("/api/estrategias/estrategia-id/ativar")
				.header("Authorization", authorization))
				.andExpect(status().isOk());
		mockMvc.perform(patch("/api/estrategias/estrategia-id/desativar")
				.header("Authorization", authorization))
				.andExpect(status().isOk());
		mockMvc.perform(delete("/api/estrategias/estrategia-id")
				.header("Authorization", authorization))
				.andExpect(status().isNoContent());
	}

	@ParameterizedTest
	@EnumSource(value = Role.class, names = { "GESTOR", "OPERADOR" })
	void gestorEOperadorRecebemProibidoEmOperacoesDeEscrita(Role role) throws Exception {
		String authorization = bearer(role);

		mockMvc.perform(post("/api/estrategias")
				.header("Authorization", authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content(BODY_VALIDO))
				.andExpect(status().isForbidden());
		mockMvc.perform(put("/api/estrategias/estrategia-id")
				.header("Authorization", authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content(BODY_VALIDO))
				.andExpect(status().isForbidden());
		mockMvc.perform(patch("/api/estrategias/estrategia-id/ativar")
				.header("Authorization", authorization))
				.andExpect(status().isForbidden());
		mockMvc.perform(patch("/api/estrategias/estrategia-id/desativar")
				.header("Authorization", authorization))
				.andExpect(status().isForbidden());
		mockMvc.perform(delete("/api/estrategias/estrategia-id")
				.header("Authorization", authorization))
				.andExpect(status().isForbidden());
		verify(estrategiaService, never()).criar(any(), anyString());
	}

	@Test
	void metodoHttpNaoPermitidoRetornaRespostaPadronizada() throws Exception {
		mockMvc.perform(post("/api/estrategias/estrategia-id")
				.header("Authorization", bearer(Role.LIDERANCA)))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.timestamp").exists())
				.andExpect(jsonPath("$.status").value(405))
				.andExpect(jsonPath("$.erro").value("Method Not Allowed"))
				.andExpect(jsonPath("$.mensagem").value("Método HTTP não permitido"))
				.andExpect(jsonPath("$.path").value("/api/estrategias/estrategia-id"))
				.andExpect(content().string(not(containsString("stackTrace"))))
				.andExpect(content().string(not(containsString("HttpRequestMethodNotSupportedException"))));
	}

	@Test
	void rotaComBarraFinalRetornaNaoEncontradaPadronizada() throws Exception {
		mockMvc.perform(get("/api/estrategias/")
				.header("Authorization", bearer(Role.OPERADOR)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.timestamp").exists())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.erro").value("Not Found"))
				.andExpect(jsonPath("$.mensagem").value("Rota não encontrada"))
				.andExpect(jsonPath("$.path").value("/api/estrategias/"))
				.andExpect(content().string(not(containsString("stackTrace"))))
				.andExpect(content().string(not(containsString("NoResourceFoundException"))));
	}

	@Test
	void rotaCompletamenteInexistenteRetornaNaoEncontradaPadronizada() throws Exception {
		mockMvc.perform(get("/api/rota-inexistente")
				.header("Authorization", bearer(Role.GESTOR)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.timestamp").exists())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.erro").value("Not Found"))
				.andExpect(jsonPath("$.mensagem").value("Rota não encontrada"))
				.andExpect(jsonPath("$.path").value("/api/rota-inexistente"))
				.andExpect(content().string(not(containsString("stackTrace"))))
				.andExpect(content().string(not(containsString("NoResourceFoundException"))));
	}

	@Test
	void tokenInvalidoContinuaRetornandoNaoAutorizado() throws Exception {
		mockMvc.perform(get("/api/estrategias")
				.header("Authorization", "Bearer token-invalido"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));
	}

	@ParameterizedTest
	@EnumSource(Role.class)
	void todosOsPerfisPodemConsultar(Role role) throws Exception {
		when(estrategiaService.listar(any(), any(), any(), anyInt(), anyInt(), eq("usuario-id")))
				.thenReturn(PaginaResponse.de(List.of(estrategiaResponse()), 0, 20, 1));
		when(estrategiaService.listarAtivas("usuario-id")).thenReturn(List.of(estrategiaResponse()));
		when(estrategiaService.buscarPorId("estrategia-id", "usuario-id")).thenReturn(estrategiaResponse());
		when(estrategiaService.consultarHistorico("estrategia-id", "usuario-id")).thenReturn(List.of());
		String authorization = bearer(role);

		mockMvc.perform(get("/api/estrategias").header("Authorization", authorization))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/estrategias/ativas").header("Authorization", authorization))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/estrategias/estrategia-id").header("Authorization", authorization))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/estrategias/estrategia-id/historico").header("Authorization", authorization))
				.andExpect(status().isOk());
	}

	@Test
	void requisicaoSemJwtRetornaNaoAutorizado() throws Exception {
		mockMvc.perform(get("/api/estrategias"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void usuarioInativoRetornaProibido() throws Exception {
		when(estrategiaService.listar(any(), any(), any(), anyInt(), anyInt(), eq("usuario-id")))
				.thenThrow(new UsuarioInativoException());

		mockMvc.perform(get("/api/estrategias").header("Authorization", bearer(Role.OPERADOR)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.mensagem").value("Usuário inativo"));
	}

	@Test
	void estrategiaInexistenteRetornaNaoEncontrada() throws Exception {
		when(estrategiaService.buscarPorId("inexistente", "usuario-id"))
				.thenThrow(new EstrategiaNaoEncontradaException());

		mockMvc.perform(get("/api/estrategias/inexistente")
				.header("Authorization", bearer(Role.GESTOR)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensagem").value("Estratégia não encontrada"));
	}

	@Test
	void operacaoInvalidaRetornaConflito() throws Exception {
		when(estrategiaService.ativar("estrategia-id", "usuario-id"))
				.thenThrow(new OperacaoEstrategiaInvalidaException("Estratégia arquivada não pode ser alterada"));

		mockMvc.perform(patch("/api/estrategias/estrategia-id/ativar")
				.header("Authorization", bearer(Role.LIDERANCA)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.mensagem").value("Estratégia arquivada não pode ser alterada"));
	}

	@Test
	void conflitoOtimistaRetornaMensagemGenerica() throws Exception {
		when(estrategiaService.ativar("estrategia-id", "usuario-id"))
				.thenThrow(new OptimisticLockingFailureException("detalhe interno"));

		mockMvc.perform(patch("/api/estrategias/estrategia-id/ativar")
				.header("Authorization", bearer(Role.LIDERANCA)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensagem").value(
						"O recurso foi alterado por outra operação. Atualize os dados e tente novamente."))
				.andExpect(content().string(not(containsString("detalhe interno"))));
	}

	@Test
	void dadosInvalidosRetornamBadRequest() throws Exception {
		mockMvc.perform(post("/api/estrategias")
				.header("Authorization", bearer(Role.LIDERANCA))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "titulo": "x",
						  "descricao": "curta",
						  "categoria": "",
						  "campanha": ""
						}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(content().string(not(containsString("stackTrace"))))
				.andExpect(content().string(not(containsString("senhaHash"))));
	}

	@Test
	void tituloComTamanhoAparentePorEspacosRetornaBadRequest() throws Exception {
		esperarTextosInvalidosNaCriacaoEAtualizacao(
				bodyEstrategia(" a ", "Descrição válida", "Categoria", "Campanha"));
	}

	@Test
	void descricaoComTamanhoAparentePorEspacosRetornaBadRequest() throws Exception {
		esperarTextosInvalidosNaCriacaoEAtualizacao(
				bodyEstrategia("Título válido", " texto    ", "Categoria", "Campanha"));
	}

	@Test
	void categoriaComTamanhoAparentePorEspacosRetornaBadRequest() throws Exception {
		esperarTextosInvalidosNaCriacaoEAtualizacao(
				bodyEstrategia("Título válido", "Descrição válida", " x ", "Campanha"));
	}

	@Test
	void campanhaComTamanhoAparentePorEspacosRetornaBadRequest() throws Exception {
		esperarTextosInvalidosNaCriacaoEAtualizacao(
				bodyEstrategia("Título válido", "Descrição válida", "Categoria", " x "));
	}

	@Test
	void valoresValidosComEspacosSaoAceitosEEntreguesNormalizadosAoServico() throws Exception {
		when(estrategiaService.criar(any(), eq("usuario-id"))).thenReturn(estrategiaResponse());
		when(estrategiaService.atualizar(eq("estrategia-id"), any(), eq("usuario-id")))
				.thenReturn(estrategiaResponse());
		String body = bodyEstrategia(
				"\u2003Estratégia válida\u2003",
				"\u2003Descrição suficientemente válida\u2003",
				"\u2003Inovação\u2003",
				"\u2003Campanha anual\u2003");
		String authorization = bearer(Role.LIDERANCA);

		mockMvc.perform(post("/api/estrategias")
				.header("Authorization", authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.andExpect(status().isCreated());
		mockMvc.perform(put("/api/estrategias/estrategia-id")
				.header("Authorization", authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.andExpect(status().isOk());

		ArgumentCaptor<CriarEstrategiaRequest> criarCaptor = ArgumentCaptor.forClass(CriarEstrategiaRequest.class);
		ArgumentCaptor<AtualizarEstrategiaRequest> atualizarCaptor = ArgumentCaptor
				.forClass(AtualizarEstrategiaRequest.class);
		verify(estrategiaService).criar(criarCaptor.capture(), eq("usuario-id"));
		verify(estrategiaService).atualizar(eq("estrategia-id"), atualizarCaptor.capture(), eq("usuario-id"));
		assertThatNormalizado(criarCaptor.getValue().titulo(), criarCaptor.getValue().descricao(),
				criarCaptor.getValue().categoria(), criarCaptor.getValue().campanha());
		assertThatNormalizado(atualizarCaptor.getValue().titulo(), atualizarCaptor.getValue().descricao(),
				atualizarCaptor.getValue().categoria(), atualizarCaptor.getValue().campanha());
	}

	@Test
	void valoresNulosContinuamRetornandoBadRequest() throws Exception {
		String body = """
				{
				  "titulo": null,
				  "descricao": null,
				  "data": null,
				  "categoria": null,
				  "campanha": null
				}
				""";
		esperarTextosInvalidosNaCriacaoEAtualizacao(body);
	}

	private void esperarTextosInvalidosNaCriacaoEAtualizacao(String body) throws Exception {
		String authorization = bearer(Role.LIDERANCA);
		mockMvc.perform(post("/api/estrategias")
				.header("Authorization", authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
		mockMvc.perform(put("/api/estrategias/estrategia-id")
				.header("Authorization", authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}

	private String bodyEstrategia(String titulo, String descricao, String categoria, String campanha) {
		return """
				{
				  "titulo": "%s",
				  "descricao": "%s",
				  "data": "2027-01-10",
				  "categoria": "%s",
				  "campanha": "%s"
				}
				""".formatted(titulo, descricao, categoria, campanha);
	}

	private void assertThatNormalizado(String titulo, String descricao, String categoria, String campanha) {
		org.assertj.core.api.Assertions.assertThat(titulo).isEqualTo("Estratégia válida");
		org.assertj.core.api.Assertions.assertThat(descricao).isEqualTo("Descrição suficientemente válida");
		org.assertj.core.api.Assertions.assertThat(categoria).isEqualTo("Inovação");
		org.assertj.core.api.Assertions.assertThat(campanha).isEqualTo("Campanha anual");
	}

	private String bearer(Role role) {
		Instant agora = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer("inovagab-api")
				.issuedAt(agora)
				.expiresAt(agora.plusSeconds(3600))
				.subject("usuario-id")
				.claim("email", "usuario@exemplo.com")
				.claim("role", role.name())
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return "Bearer " + token;
	}

	private EstrategiaResponse estrategiaResponse() {
		return new EstrategiaResponse(
				"estrategia-id",
				"Estratégia 2027",
				"Descrição completa da estratégia",
				LocalDate.of(2027, 1, 10),
				"Inovação",
				"Campanha anual",
				StatusEstrategia.RASCUNHO,
				"usuario-id",
				"usuario-id",
				Instant.parse("2027-01-01T10:00:00Z"),
				Instant.parse("2027-01-01T10:00:00Z"),
				0L);
	}
}
