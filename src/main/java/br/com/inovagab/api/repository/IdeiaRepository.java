package br.com.inovagab.api.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import br.com.inovagab.api.model.Ideia;

public interface IdeiaRepository extends MongoRepository<Ideia, String> {
}
