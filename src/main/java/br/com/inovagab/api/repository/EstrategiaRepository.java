package br.com.inovagab.api.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import br.com.inovagab.api.model.Estrategia;
import br.com.inovagab.api.model.StatusEstrategia;

public interface EstrategiaRepository extends MongoRepository<Estrategia, String> {

	List<Estrategia> findByStatusOrderByDataDesc(StatusEstrategia status);
}
