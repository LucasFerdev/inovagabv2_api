package br.com.inovagab.api.service;

import org.springframework.stereotype.Service;

import br.com.inovagab.api.exception.UsuarioInativoException;
import br.com.inovagab.api.exception.UsuarioNaoEncontradoException;
import br.com.inovagab.api.model.Usuario;
import br.com.inovagab.api.repository.UsuarioRepository;

@Service
public class UsuarioAutenticadoService {

	private final UsuarioRepository usuarioRepository;

	public UsuarioAutenticadoService(UsuarioRepository usuarioRepository) {
		this.usuarioRepository = usuarioRepository;
	}

	public Usuario buscarAtivo(String usuarioId) {
		Usuario usuario = usuarioRepository.findById(usuarioId)
				.orElseThrow(UsuarioNaoEncontradoException::new);
		if (!usuario.isAtivo()) {
			throw new UsuarioInativoException();
		}
		return usuario;
	}
}
