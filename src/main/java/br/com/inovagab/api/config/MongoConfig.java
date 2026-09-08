package br.com.inovagab.api.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

import br.com.inovagab.api.model.Estrategia;
import br.com.inovagab.api.model.Ideia;
import br.com.inovagab.api.model.Projeto;
import br.com.inovagab.api.model.Usuario;

@Configuration
@EnableMongoAuditing
public class MongoConfig {

	@Bean
	public ApplicationRunner indicesMongo(MongoTemplate mongoTemplate) {
		return args -> {
			mongoTemplate.indexOps(Usuario.class)
					.createIndex(new Index().on("email", Sort.Direction.ASC).unique());
			mongoTemplate.indexOps(Estrategia.class)
					.createIndex(new Index().on("status", Sort.Direction.ASC));
			mongoTemplate.indexOps(Estrategia.class)
					.createIndex(new Index().on("categoria", Sort.Direction.ASC));
			mongoTemplate.indexOps(Estrategia.class)
					.createIndex(new Index().on("campanha", Sort.Direction.ASC));
			mongoTemplate.indexOps(Estrategia.class)
					.createIndex(new Index().on("data", Sort.Direction.DESC));
			mongoTemplate.indexOps(Ideia.class)
					.createIndex(new Index().on("autorId", Sort.Direction.ASC));
			mongoTemplate.indexOps(Ideia.class)
					.createIndex(new Index().on("status", Sort.Direction.ASC));
			mongoTemplate.indexOps(Ideia.class)
					.createIndex(new Index().on("estrategiaId", Sort.Direction.ASC));
			mongoTemplate.indexOps(Ideia.class)
					.createIndex(new Index().on("categoria", Sort.Direction.ASC));
			mongoTemplate.indexOps(Ideia.class)
					.createIndex(new Index().on("prioridade", Sort.Direction.ASC));
			mongoTemplate.indexOps(Ideia.class)
					.createIndex(new Index().on("criadoEm", Sort.Direction.DESC));
			mongoTemplate.indexOps(Projeto.class).createIndex(new Index().on("estrategiaId", Sort.Direction.ASC));
			mongoTemplate.indexOps(Projeto.class).createIndex(new Index().on("ideiaOrigemId", Sort.Direction.ASC));
			mongoTemplate.indexOps(Projeto.class).createIndex(new Index().on("status", Sort.Direction.ASC));
			mongoTemplate.indexOps(Projeto.class).createIndex(new Index().on("etapa", Sort.Direction.ASC));
			mongoTemplate.indexOps(Projeto.class)
					.createIndex(new Index().on("gestorResponsavelId", Sort.Direction.ASC));
			mongoTemplate.indexOps(Projeto.class).createIndex(new Index().on("prazo", Sort.Direction.ASC));
			mongoTemplate.indexOps(Projeto.class).createIndex(new Index().on("criadoEm", Sort.Direction.DESC));
		};
	}
}
