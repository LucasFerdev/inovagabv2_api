package br.com.inovagab.api.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import br.com.inovagab.api.model.Usuario;

public interface UsuarioRepository extends MongoRepository<Usuario, String> {

	Optional<Usuario> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);
}
