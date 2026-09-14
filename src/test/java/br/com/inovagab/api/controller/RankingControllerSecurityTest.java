package br.com.inovagab.api.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.inovagab.api.config.JwtConfig;
import br.com.inovagab.api.config.SecurityConfig;
import br.com.inovagab.api.dto.MedalhaRanking;
import br.com.inovagab.api.dto.RankingColaboradorResponse;
import br.com.inovagab.api.exception.GlobalExceptionHandler;
import br.com.inovagab.api.model.Role;
import br.com.inovagab.api.security.SecurityErrorHandler;
import br.com.inovagab.api.service.RankingService;

@WebMvcTest(RankingController.class)
@Import({ SecurityConfig.class, JwtConfig.class, SecurityErrorHandler.class, GlobalExceptionHandler.class })
@TestPropertySource(properties = { "app.jwt.secret=c2VncmVkby1kZS10ZXN0ZS1jb20tbWFpcy1kZS0yNTYtYml0cw==",
		"app.jwt.expiration-seconds=3600", "app.jwt.issuer=inovagab-api" })
class RankingControllerSecurityTest {

	@Autowired private MockMvc mockMvc;
	@Autowired private JwtEncoder jwtEncoder;
	@MockitoBean private RankingService rankingService;

	@ParameterizedTest
	@EnumSource(Role.class)
	void qualquerPerfilAutenticadoRecebe200(Role role) throws Exception {
		when(rankingService.colaboradores("usuario-id")).thenReturn(List.of());

		mockMvc.perform(get("/api/ranking/colaboradores").header("Authorization", bearer(role)))
				.andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
	}

	@Test
	void semTokenRecebe401() throws Exception {
		mockMvc.perform(get("/api/ranking/colaboradores"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void respostaContemSomenteCamposDoContratoSemDadosSensiveis() throws Exception {
		when(rankingService.colaboradores("usuario-id")).thenReturn(List.of(
				new RankingColaboradorResponse(1, "Ana", "Empresa A", 3, 2, MedalhaRanking.OURO)));

		mockMvc.perform(get("/api/ranking/colaboradores").header("Authorization", bearer(Role.OPERADOR)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].*", hasSize(6)))
				.andExpect(jsonPath("$[0].posicao").value(1))
				.andExpect(jsonPath("$[0].nome").value("Ana"))
				.andExpect(jsonPath("$[0].empresa").value("Empresa A"))
				.andExpect(jsonPath("$[0].ideiasAprovadas").value(3))
				.andExpect(jsonPath("$[0].ideiasImplementadas").value(2))
				.andExpect(jsonPath("$[0].medalha").value("OURO"))
				.andExpect(jsonPath("$[0].email").doesNotExist())
				.andExpect(jsonPath("$[0].senhaHash").doesNotExist())
				.andExpect(jsonPath("$[0].token").doesNotExist())
				.andExpect(jsonPath("$[0].id").doesNotExist())
				.andExpect(jsonPath("$[0].autorId").doesNotExist());
	}

	private String bearer(Role role) {
		Instant agora = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder().issuer("inovagab-api").issuedAt(agora)
				.expiresAt(agora.plusSeconds(3600)).subject("usuario-id").claim("role", role.name()).build();
		return "Bearer " + jwtEncoder.encode(JwtEncoderParameters.from(
				JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
	}
}
