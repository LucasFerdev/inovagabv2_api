package br.com.inovagab.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Base64;

import org.junit.jupiter.api.Test;
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
import br.com.inovagab.api.dto.LoginResponse;
import br.com.inovagab.api.dto.UsuarioResponse;
import br.com.inovagab.api.exception.CredenciaisInvalidasException;
import br.com.inovagab.api.exception.EmailDuplicadoException;
import br.com.inovagab.api.exception.GlobalExceptionHandler;
import br.com.inovagab.api.exception.UsuarioInativoException;
import br.com.inovagab.api.exception.UsuarioNaoEncontradoException;
import br.com.inovagab.api.model.Role;
import br.com.inovagab.api.security.JwtProperties;
import br.com.inovagab.api.security.SecurityErrorHandler;
import br.com.inovagab.api.service.AuthService;

@WebMvcTest(AuthController.class)
@Import({ SecurityConfig.class, JwtConfig.class, SecurityErrorHandler.class, GlobalExceptionHandler.class })
@TestPropertySource(properties = {
		"app.jwt.secret=c2VncmVkby1kZS10ZXN0ZS1jb20tbWFpcy1kZS0yNTYtYml0cw==",
		"app.jwt.expiration-seconds=3600",
		"app.jwt.issuer=inovagab-api"
})
class AuthControllerSecurityTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtEncoder jwtEncoder;

	@MockitoBean
	private AuthService authService;

	@Test
	void cadastroPublicoRetornaCriadoSemSenhaHash() throws Exception {
		when(authService.cadastrar(any())).thenReturn(usuarioResponse());

		mockMvc.perform(post("/api/auth/cadastro")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "nome": "Usuário Teste",
						  "email": "usuario@exemplo.com",
						  "senha": "senha-segura",
						  "empresa": "Empresa Teste"
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.role").value("OPERADOR"))
				.andExpect(jsonPath("$.senhaHash").doesNotExist())
				.andExpect(jsonPath("$.senha").doesNotExist());
	}

	@Test
	void loginPublicoRetornaJwtSemSenhaHash() throws Exception {
		when(authService.login(any())).thenReturn(new LoginResponse("jwt-de-teste", "Bearer", 3600, usuarioResponse()));

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "usuario@exemplo.com",
						  "senha": "senha-segura"
						}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").value("jwt-de-teste"))
				.andExpect(jsonPath("$.usuario.senhaHash").doesNotExist())
				.andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("senhaHash"))));
	}

	@Test
	void emailDuplicadoRetornaConflito() throws Exception {
		when(authService.cadastrar(any())).thenThrow(new EmailDuplicadoException());

		mockMvc.perform(post("/api/auth/cadastro")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "nome": "Usuário Teste",
						  "email": "usuario@exemplo.com",
						  "senha": "senha-segura",
						  "empresa": "Empresa Teste"
						}
						"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.path").value("/api/auth/cadastro"));
	}

	@Test
	void credenciaisInvalidasRetornamNaoAutorizado() throws Exception {
		when(authService.login(any())).thenThrow(new CredenciaisInvalidasException());

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "usuario@exemplo.com",
						  "senha": "senha-incorreta"
						}
						"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos"));
	}

	@Test
	void cadastroInvalidoRetornaErroPadronizado() throws Exception {
		mockMvc.perform(post("/api/auth/cadastro")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "nome": "",
						  "email": "email-invalido",
						  "senha": "curta",
						  "empresa": ""
						}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.timestamp").exists())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.erro").value("Bad Request"))
				.andExpect(jsonPath("$.mensagem").isNotEmpty())
				.andExpect(jsonPath("$.path").value("/api/auth/cadastro"));
	}

	@Test
	void jsonQuebradoNoCadastroRetornaErroSeguro() throws Exception {
		String segredoEnviado = "SEGREDO_QUE_NAO_PODE_VAZAR";

		mockMvc.perform(post("/api/auth/cadastro")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Usuário\",\"senha\":\"" + segredoEnviado + "\""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensagem").value("Corpo da requisição inválido"))
				.andExpect(jsonPath("$.path").value("/api/auth/cadastro"))
				.andExpect(content().string(not(containsString(segredoEnviado))))
				.andExpect(content().string(not(containsString("senhaHash"))))
				.andExpect(content().string(not(containsString("stackTrace"))))
				.andExpect(content().string(not(containsString("Json"))));
	}

	@Test
	void jsonQuebradoNoLoginRetornaErroSeguro() throws Exception {
		String segredoEnviado = "OUTRO_SEGREDO_QUE_NAO_PODE_VAZAR";

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"usuario@exemplo.com\",\"senha\":\"" + segredoEnviado + "\""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensagem").value("Corpo da requisição inválido"))
				.andExpect(jsonPath("$.path").value("/api/auth/login"))
				.andExpect(content().string(not(containsString(segredoEnviado))))
				.andExpect(content().string(not(containsString("senhaHash"))))
				.andExpect(content().string(not(containsString("stackTrace"))))
				.andExpect(content().string(not(containsString("Jackson"))));
	}

	@Test
	void senhaLongaNoCadastroRetornaErroSemExporValor() throws Exception {
		String senhaLonga = "S".repeat(73);

		mockMvc.perform(post("/api/auth/cadastro")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "nome": "Usuário Teste",
						  "email": "usuario@exemplo.com",
						  "senha": "%s",
						  "empresa": "Empresa Teste"
						}
						""".formatted(senhaLonga)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensagem").value("senha: A senha deve ter entre 8 e 72 caracteres"))
				.andExpect(content().string(not(containsString(senhaLonga))))
				.andExpect(content().string(not(containsString("senhaHash"))))
				.andExpect(content().string(not(containsString("stackTrace"))));
	}

	@Test
	void senhaLongaNoLoginRetornaErroSemExporValor() throws Exception {
		String senhaLonga = "S".repeat(73);

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "usuario@exemplo.com",
						  "senha": "%s"
						}
						""".formatted(senhaLonga)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensagem").value("senha: A senha deve ter no máximo 72 caracteres"))
				.andExpect(content().string(not(containsString(senhaLonga))))
				.andExpect(content().string(not(containsString("senhaHash"))))
				.andExpect(content().string(not(containsString("stackTrace"))));
	}

	@Test
	void meExigeAutenticacao() throws Exception {
		mockMvc.perform(get("/api/auth/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void jwtInvalidoERejeitado() throws Exception {
		mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer token-invalido"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.mensagem").value("Token ausente ou inválido"));
	}

	@Test
	void jwtExpiradoERejeitado() throws Exception {
		Instant agora = Instant.now();
		String tokenExpirado = gerarToken(agora.minusSeconds(7200), agora.minusSeconds(3600));

		mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + tokenExpirado))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.mensagem").value("Token ausente ou inválido"));
	}

	@Test
	void jwtComAssinaturaIncorretaERejeitado() throws Exception {
		JwtProperties outrasProperties = new JwtProperties(
				Base64.getEncoder().encodeToString("outro-segredo-de-teste-com-256-bits".getBytes()),
				3600,
				"inovagab-api");
		JwtEncoder outroEncoder = new JwtConfig().jwtEncoder(outrasProperties);
		Instant agora = Instant.now();
		String token = gerarToken(outroEncoder, agora, agora.plusSeconds(3600));

		mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.mensagem").value("Token ausente ou inválido"));
	}

	@Test
	void meComJwtValidoRetornaUsuarioSeguro() throws Exception {
		when(authService.buscarUsuario("usuario-id")).thenReturn(usuarioResponse());
		Instant agora = Instant.now();
		String token = gerarToken(agora, agora.plusSeconds(3600));

		mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value("usuario-id"))
				.andExpect(jsonPath("$.senhaHash").doesNotExist());
	}

	@Test
	void meRetornaProibidoParaUsuarioInativo() throws Exception {
		when(authService.buscarUsuario("usuario-id")).thenThrow(new UsuarioInativoException());
		Instant agora = Instant.now();
		String token = gerarToken(agora, agora.plusSeconds(3600));

		mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.erro").value("Forbidden"))
				.andExpect(jsonPath("$.mensagem").value("Usuário inativo"))
				.andExpect(jsonPath("$.path").value("/api/auth/me"))
				.andExpect(content().string(not(containsString("senhaHash"))));
	}

	@Test
	void meRetornaNaoEncontradoQuandoUsuarioDoTokenNaoExiste() throws Exception {
		when(authService.buscarUsuario("usuario-id")).thenThrow(new UsuarioNaoEncontradoException());
		Instant agora = Instant.now();
		String token = gerarToken(agora, agora.plusSeconds(3600));

		mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.mensagem").value("Usuário não encontrado"))
				.andExpect(jsonPath("$.path").value("/api/auth/me"));
	}

	private String gerarToken(Instant emitidoEm, Instant expiraEm) {
		return gerarToken(jwtEncoder, emitidoEm, expiraEm);
	}

	private String gerarToken(JwtEncoder encoder, Instant emitidoEm, Instant expiraEm) {
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer("inovagab-api")
				.issuedAt(emitidoEm)
				.expiresAt(expiraEm)
				.subject("usuario-id")
				.claim("email", "usuario@exemplo.com")
				.claim("role", "OPERADOR")
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	private UsuarioResponse usuarioResponse() {
		return new UsuarioResponse(
				"usuario-id",
				"Usuário Teste",
				"usuario@exemplo.com",
				"Empresa Teste",
				Role.OPERADOR,
				true,
				Instant.parse("2026-09-06T12:00:00Z"),
				Instant.parse("2026-09-06T12:00:00Z"));
	}
}
