package br.com.inovagab.api.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

import br.com.inovagab.api.model.Estrategia;
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
		};
	}
}
