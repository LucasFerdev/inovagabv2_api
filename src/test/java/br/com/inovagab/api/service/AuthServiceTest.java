package br.com.inovagab.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.com.inovagab.api.config.CadastroProperties;
import br.com.inovagab.api.config.JwtConfig;
import br.com.inovagab.api.dto.CadastroUsuarioRequest;
import br.com.inovagab.api.dto.LoginRequest;
import br.com.inovagab.api.dto.LoginResponse;
import br.com.inovagab.api.dto.UsuarioResponse;
import br.com.inovagab.api.exception.CodigoAcessoInvalidoException;
import br.com.inovagab.api.exception.CredenciaisInvalidasException;
import br.com.inovagab.api.exception.EmailDuplicadoException;
import br.com.inovagab.api.exception.UsuarioInativoException;
import br.com.inovagab.api.exception.UsuarioNaoEncontradoException;
import br.com.inovagab.api.model.Role;
import br.com.inovagab.api.model.Usuario;
import br.com.inovagab.api.repository.UsuarioRepository;
import br.com.inovagab.api.security.JwtProperties;
import br.com.inovagab.api.security.JwtService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	private static final String SEGREDO = Base64.getEncoder()
			.encodeToString("segredo-de-teste-com-mais-de-256-bits".getBytes());

	@Mock
	private UsuarioRepository usuarioRepository;

	private PasswordEncoder passwordEncoder;
	private AuthService authService;
	private JwtService jwtService;

	@BeforeEach
	void configurar() {
		passwordEncoder = new BCryptPasswordEncoder();
		JwtProperties properties = new JwtProperties(SEGREDO, 3600, "inovagab-api");
		JwtConfig jwtConfig = new JwtConfig();
		jwtService = new JwtService(jwtConfig.jwtEncoder(properties), properties);
		authService = new AuthService(
				usuarioRepository,
				passwordEncoder,
				jwtService,
				new UsuarioAutenticadoService(usuarioRepository),
				new CadastroProperties("", ""));
	}

	@Test
	void cadastroCriptografaSenhaNormalizaEmailESempreCriaOperador() {
		when(usuarioRepository.existsByEmailIgnoreCase("usuario@exemplo.com")).thenReturn(false);
		when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
			Usuario usuario = invocation.getArgument(0);
			usuario.setId("usuario-id");
			usuario.setCriadoEm(Instant.parse("2026-09-06T12:00:00Z"));
			usuario.setAtualizadoEm(Instant.parse("2026-09-06T12:00:00Z"));
			return usuario;
		});

		UsuarioResponse response = authService.cadastrar(new CadastroUsuarioRequest(
				" Usuário Teste ", " Usuario@Exemplo.COM ", "senha-segura", " Empresa Teste "));

		ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
		verify(usuarioRepository).save(captor.capture());
		Usuario salvo = captor.getValue();
		assertThat(salvo.getEmail()).isEqualTo("usuario@exemplo.com");
		assertThat(salvo.getRole()).isEqualTo(Role.OPERADOR);
		assertThat(salvo.getSenhaHash()).isNotEqualTo("senha-segura");
		assertThat(passwordEncoder.matches("senha-segura", salvo.getSenhaHash())).isTrue();
		assertThat(response.email()).isEqualTo("usuario@exemplo.com");
		assertThat(response.role()).isEqualTo(Role.OPERADOR);
	}

	@Test
	void cadastroComEmailDuplicadoRetornaConflito() {
		when(usuarioRepository.existsByEmailIgnoreCase("usuario@exemplo.com")).thenReturn(true);

		assertThatThrownBy(() -> authService.cadastrar(new CadastroUsuarioRequest(
				"Usuário", " USUARIO@EXEMPLO.COM ", "senha-segura", "Empresa")))
				.isInstanceOf(EmailDuplicadoException.class);
		verify(usuarioRepository, never()).save(any());
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "", "   " })
	void cadastroComCodigoAusenteOuEmBrancoCriaOperador(String codigoAcesso) {
		configurarPersistenciaDoCadastro();

		UsuarioResponse response = authService.cadastrar(new CadastroUsuarioRequest(
				"Usuário", "usuario@exemplo.com", "senha-segura", "Empresa", codigoAcesso));

		assertThat(response.role()).isEqualTo(Role.OPERADOR);
	}

	@Test
	void cadastroComCodigoValidoDeGestorCriaGestor() {
		authService = authServiceComCodigos("codigo-gestor-teste", "codigo-lideranca-teste");
		configurarPersistenciaDoCadastro();

		UsuarioResponse response = authService.cadastrar(new CadastroUsuarioRequest(
				"Usuário", "usuario@exemplo.com", "senha-segura", "Empresa", "  codigo-gestor-teste  "));

		assertThat(response.role()).isEqualTo(Role.GESTOR);
	}

	@Test
	void cadastroComCodigoValidoDeLiderancaCriaLideranca() {
		authService = authServiceComCodigos("codigo-gestor-teste", "codigo-lideranca-teste");
		configurarPersistenciaDoCadastro();

		UsuarioResponse response = authService.cadastrar(new CadastroUsuarioRequest(
				"Usuário", "usuario@exemplo.com", "senha-segura", "Empresa", "codigo-lideranca-teste"));

		assertThat(response.role()).isEqualTo(Role.LIDERANCA);
	}

	@Test
	void cadastroComCodigoInvalidoNaoSalvaUsuario() {
		authService = authServiceComCodigos("codigo-gestor-teste", "codigo-lideranca-teste");

		assertThatThrownBy(() -> authService.cadastrar(new CadastroUsuarioRequest(
				"Usuário", "usuario@exemplo.com", "senha-segura", "Empresa", "codigo-invalido-teste")))
				.isInstanceOf(CodigoAcessoInvalidoException.class)
				.hasMessage("Código de acesso inválido")
				.hasMessageNotContaining("codigo-invalido-teste");
		verify(usuarioRepository, never()).save(any());
	}

	@Test
	void cadastroRequestNaoExpoeCodigoEmTexto() {
		String codigo = "codigo-confidencial-teste";
		CadastroUsuarioRequest request = new CadastroUsuarioRequest(
				"Usuário", "usuario@exemplo.com", "senha-segura", "Empresa", codigo);

		assertThat(request.toString()).doesNotContain(codigo);
	}

	@Test
	void loginContinuaRetornandoRoleCadastrada() {
		authService = authServiceComCodigos("codigo-gestor-teste", "codigo-lideranca-teste");
		configurarPersistenciaDoCadastro();
		UsuarioResponse cadastro = authService.cadastrar(new CadastroUsuarioRequest(
				"Usuário", "usuario@exemplo.com", "senha-segura", "Empresa", "codigo-gestor-teste"));
		ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
		verify(usuarioRepository).save(captor.capture());
		when(usuarioRepository.findByEmailIgnoreCase("usuario@exemplo.com")).thenReturn(Optional.of(captor.getValue()));

		LoginResponse login = authService.login(new LoginRequest("usuario@exemplo.com", "senha-segura"));

		assertThat(cadastro.role()).isEqualTo(Role.GESTOR);
		assertThat(login.usuario().role()).isEqualTo(Role.GESTOR);
	}

	@Test
	void loginValidoRetornaJwt() {
		Usuario usuario = usuarioAtivo();
		when(usuarioRepository.findByEmailIgnoreCase("usuario@exemplo.com")).thenReturn(Optional.of(usuario));

		LoginResponse response = authService.login(new LoginRequest(" Usuario@Exemplo.COM ", "senha-segura"));

		assertThat(response.token()).hasSizeGreaterThan(100).contains(".");
		assertThat(response.tipo()).isEqualTo("Bearer");
		assertThat(response.expiresIn()).isEqualTo(3600);
		assertThat(response.usuario().id()).isEqualTo("usuario-id");
	}

	@Test
	void loginComSenhaIncorretaRetornaNaoAutorizado() {
		when(usuarioRepository.findByEmailIgnoreCase("usuario@exemplo.com")).thenReturn(Optional.of(usuarioAtivo()));

		assertThatThrownBy(() -> authService.login(new LoginRequest("usuario@exemplo.com", "senha-incorreta")))
				.isInstanceOf(CredenciaisInvalidasException.class)
				.hasMessage("E-mail ou senha inválidos");
	}

	@Test
	void usuarioInativoNaoConsegueEntrar() {
		Usuario usuario = usuarioAtivo();
		usuario.setAtivo(false);
		when(usuarioRepository.findByEmailIgnoreCase("usuario@exemplo.com")).thenReturn(Optional.of(usuario));

		assertThatThrownBy(() -> authService.login(new LoginRequest("usuario@exemplo.com", "senha-segura")))
				.isInstanceOf(CredenciaisInvalidasException.class);
	}

	@Test
	void buscaDoUsuarioAutenticadoRejeitaUsuarioInativo() {
		Usuario usuario = usuarioAtivo();
		usuario.setAtivo(false);
		when(usuarioRepository.findById("usuario-id")).thenReturn(Optional.of(usuario));

		assertThatThrownBy(() -> authService.buscarUsuario("usuario-id"))
				.isInstanceOf(UsuarioInativoException.class)
				.hasMessage("Usuário inativo");
	}

	@Test
	void buscaDoUsuarioAutenticadoInexistenteRetornaNaoEncontrado() {
		when(usuarioRepository.findById("usuario-id")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.buscarUsuario("usuario-id"))
				.isInstanceOf(UsuarioNaoEncontradoException.class);
	}

	private Usuario usuarioAtivo() {
		return new Usuario(
				"usuario-id",
				"Usuário Teste",
				"usuario@exemplo.com",
				passwordEncoder.encode("senha-segura"),
				"Empresa Teste",
				Role.OPERADOR,
				true,
				Instant.parse("2026-09-06T12:00:00Z"),
				Instant.parse("2026-09-06T12:00:00Z"));
	}

	private AuthService authServiceComCodigos(String codigoGestor, String codigoLideranca) {
		return new AuthService(
				usuarioRepository,
				passwordEncoder,
				jwtService,
				new UsuarioAutenticadoService(usuarioRepository),
				new CadastroProperties(codigoGestor, codigoLideranca));
	}

	private void configurarPersistenciaDoCadastro() {
		when(usuarioRepository.existsByEmailIgnoreCase("usuario@exemplo.com")).thenReturn(false);
		when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
			Usuario usuario = invocation.getArgument(0);
			usuario.setId("usuario-id");
			return usuario;
		});
	}
}
