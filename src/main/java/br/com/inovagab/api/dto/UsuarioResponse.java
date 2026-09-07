package br.com.inovagab.api.dto;

import java.time.Instant;

import br.com.inovagab.api.model.Role;
import br.com.inovagab.api.model.Usuario;

public record UsuarioResponse(
		String id,
		String nome,
		String email,
		String empresa,
		Role role,
		boolean ativo,
		Instant criadoEm,
		Instant atualizadoEm) {

	public static UsuarioResponse de(Usuario usuario) {
		return new UsuarioResponse(
				usuario.getId(),
				usuario.getNome(),
				usuario.getEmail(),
				usuario.getEmpresa(),
				usuario.getRole(),
				usuario.isAtivo(),
				usuario.getCriadoEm(),
				usuario.getAtualizadoEm());
	}
}
