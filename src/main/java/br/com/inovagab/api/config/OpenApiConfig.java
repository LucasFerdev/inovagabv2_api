package br.com.inovagab.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

	private static final String BEARER_AUTH = "bearerAuth";

	@Bean
	public OpenAPI inovagabOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("InovaGAB API")
						.version("1.0.0")
						.description("API para autenticação, estratégias, ideias, projetos, dashboard e análise consultiva por IA."))
				.components(new Components().addSecuritySchemes(BEARER_AUTH,
						new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
				.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
	}
}
