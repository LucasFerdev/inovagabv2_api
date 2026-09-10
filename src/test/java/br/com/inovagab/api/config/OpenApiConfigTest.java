package br.com.inovagab.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.swagger.v3.oas.models.OpenAPI;

class OpenApiConfigTest {

	@Test
	void configuraMetadadosESegurancaBearerJwt() {
		OpenAPI openApi = new OpenApiConfig().inovagabOpenApi();

		assertThat(openApi.getInfo().getTitle()).isEqualTo("InovaGAB API");
		assertThat(openApi.getInfo().getVersion()).isEqualTo("1.0.0");
		assertThat(openApi.getComponents().getSecuritySchemes()).containsKey("bearerAuth");
		assertThat(openApi.getSecurity()).singleElement().satisfies(
				requisito -> assertThat(requisito).containsKey("bearerAuth"));
	}
}
