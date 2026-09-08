package br.com.inovagab.api.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import br.com.inovagab.api.model.Projeto;

public interface ProjetoRepository extends MongoRepository<Projeto, String> {
}
