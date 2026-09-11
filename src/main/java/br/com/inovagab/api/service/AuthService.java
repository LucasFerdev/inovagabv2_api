package br.com.inovagab.api.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import br.com.inovagab.api.config.CadastroProperties;
import br.com.inovagab.api.dto.CadastroUsuarioRequest;
import br.com.inovagab.api.dto.LoginRequest;
import br.com.inovagab.api.dto.LoginResponse;
import br.com.inovagab.api.dto.UsuarioResponse;
import br.com.inovagab.api.exception.CodigoAcessoInvalidoException;
import br.com.inovagab.api.exception.CredenciaisInvalidasException;
import br.com.inovagab.api.exception.EmailDuplicadoException;
import br.com.inovagab.api.model.Role;
import br.com.inovagab.api.model.Usuario;
import br.com.inovagab.api.repository.UsuarioRepository;
import br.com.inovagab.api.security.JwtService;

@Service
public class AuthService {

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final UsuarioAutenticadoService usuarioAutenticadoService;
	private final CadastroProperties cadastroProperties;

	public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
			UsuarioAutenticadoService usuarioAutenticadoService, CadastroProperties cadastroProperties) {
		this.usuarioRepository = usuarioRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.usuarioAutenticadoService = usuarioAutenticadoService;
		this.cadastroProperties = cadastroProperties;
	}

	public UsuarioResponse cadastrar(CadastroUsuarioRequest request) {
		Role role = determinarRole(request.codigoAcesso());
		String email = normalizarEmail(request.email());
		if (usuarioRepository.existsByEmailIgnoreCase(email)) {
			throw new EmailDuplicadoException();
		}

		Usuario usuario = new Usuario();
		usuario.setNome(request.nome().trim());
		usuario.setEmail(email);
		usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
		usuario.setEmpresa(request.empresa().trim());
		usuario.setRole(role);
		usuario.setAtivo(true);

		try {
			return UsuarioResponse.de(usuarioRepository.save(usuario));
		} catch (DuplicateKeyException exception) {
			throw new EmailDuplicadoException();
		}
	}

	public LoginResponse login(LoginRequest request) {
		String email = normalizarEmail(request.email());
		Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
				.filter(Usuario::isAtivo)
				.filter(encontrado -> passwordEncoder.matches(request.senha(), encontrado.getSenhaHash()))
				.orElseThrow(CredenciaisInvalidasException::new);

		String token = jwtService.gerarToken(usuario);
		return new LoginResponse(token, "Bearer", jwtService.getExpirationSeconds(), UsuarioResponse.de(usuario));
	}

	public UsuarioResponse buscarUsuario(String id) {
		return UsuarioResponse.de(usuarioAutenticadoService.buscarAtivo(id));
	}

	private String normalizarEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private Role determinarRole(String codigoInformado) {
		if (codigoInformado == null || codigoInformado.strip().isEmpty()) {
			return Role.OPERADOR;
		}

		String codigoNormalizado = codigoInformado.strip();
		boolean codigoGestorValido = codigoConfiguradoEIgual(cadastroProperties.codigoGestor(), codigoNormalizado);
		boolean codigoLiderancaValido = codigoConfiguradoEIgual(cadastroProperties.codigoLideranca(), codigoNormalizado);

		if (codigoGestorValido) {
			return Role.GESTOR;
		}
		if (codigoLiderancaValido) {
			return Role.LIDERANCA;
		}
		throw new CodigoAcessoInvalidoException();
	}

	private boolean codigoConfiguradoEIgual(String codigoConfigurado, String codigoInformado) {
		byte[] hashConfigurado = hash(codigoConfigurado);
		byte[] hashInformado = hash(codigoInformado);
		return !codigoConfigurado.isEmpty() && MessageDigest.isEqual(hashConfigurado, hashInformado);
	}

	private byte[] hash(String valor) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
		} catch (java.security.NoSuchAlgorithmException exception) {
			throw new IllegalStateException("Algoritmo de comparação indisponível", exception);
		}
	}
}
