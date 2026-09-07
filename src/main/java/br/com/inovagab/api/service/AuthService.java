package br.com.inovagab.api.service;

import java.util.Locale;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import br.com.inovagab.api.dto.CadastroUsuarioRequest;
import br.com.inovagab.api.dto.LoginRequest;
import br.com.inovagab.api.dto.LoginResponse;
import br.com.inovagab.api.dto.UsuarioResponse;
import br.com.inovagab.api.exception.CredenciaisInvalidasException;
import br.com.inovagab.api.exception.EmailDuplicadoException;
import br.com.inovagab.api.exception.UsuarioInativoException;
import br.com.inovagab.api.exception.UsuarioNaoEncontradoException;
import br.com.inovagab.api.model.Role;
import br.com.inovagab.api.model.Usuario;
import br.com.inovagab.api.repository.UsuarioRepository;
import br.com.inovagab.api.security.JwtService;

@Service
public class AuthService {

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.usuarioRepository = usuarioRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	public UsuarioResponse cadastrar(CadastroUsuarioRequest request) {
		String email = normalizarEmail(request.email());
		if (usuarioRepository.existsByEmailIgnoreCase(email)) {
			throw new EmailDuplicadoException();
		}

		Usuario usuario = new Usuario();
		usuario.setNome(request.nome().trim());
		usuario.setEmail(email);
		usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
		usuario.setEmpresa(request.empresa().trim());
		usuario.setRole(Role.OPERADOR);
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
		Usuario usuario = usuarioRepository.findById(id)
				.orElseThrow(UsuarioNaoEncontradoException::new);
		if (!usuario.isAtivo()) {
			throw new UsuarioInativoException();
		}
		return UsuarioResponse.de(usuario);
	}

	private String normalizarEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
