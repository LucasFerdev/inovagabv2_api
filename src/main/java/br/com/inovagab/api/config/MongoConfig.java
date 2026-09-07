package br.com.inovagab.api.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

import br.com.inovagab.api.model.Usuario;

@Configuration
@EnableMongoAuditing
public class MongoConfig {

	@Bean
	public ApplicationRunner usuarioEmailIndex(MongoTemplate mongoTemplate) {
		return args -> mongoTemplate.indexOps(Usuario.class)
				.createIndex(new Index().on("email", Sort.Direction.ASC).unique());
	}
}
