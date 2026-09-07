package br.com.inovagab.api.dto;

public record LoginResponse(String token, String tipo, long expiresIn, UsuarioResponse usuario) {
}
