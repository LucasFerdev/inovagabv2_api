package br.com.inovagab.api.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.inovagab.api.dto.CadastroUsuarioRequest;
import br.com.inovagab.api.dto.LoginRequest;
import br.com.inovagab.api.dto.LoginResponse;
import br.com.inovagab.api.dto.UsuarioResponse;
import br.com.inovagab.api.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/cadastro")
	public ResponseEntity<UsuarioResponse> cadastrar(@Valid @RequestBody CadastroUsuarioRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(authService.cadastrar(request));
	}

	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

	@GetMapping("/me")
	public UsuarioResponse usuarioAutenticado(@AuthenticationPrincipal Jwt jwt) {
		return authService.buscarUsuario(jwt.getSubject());
	}
}
