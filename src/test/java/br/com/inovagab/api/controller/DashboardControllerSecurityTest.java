package br.com.inovagab.api.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

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
import br.com.inovagab.api.dto.DashboardResumoResponse;
import br.com.inovagab.api.exception.GlobalExceptionHandler;
import br.com.inovagab.api.model.Role;
import br.com.inovagab.api.security.SecurityErrorHandler;
import br.com.inovagab.api.service.DashboardService;

@WebMvcTest(DashboardController.class)
@Import({ SecurityConfig.class, JwtConfig.class, SecurityErrorHandler.class, GlobalExceptionHandler.class })
@TestPropertySource(properties = { "app.jwt.secret=c2VncmVkby1kZS10ZXN0ZS1jb20tbWFpcy1kZS0yNTYtYml0cw==",
		"app.jwt.expiration-seconds=3600", "app.jwt.issuer=inovagab-api" })
class DashboardControllerSecurityTest {

	@Autowired private MockMvc mockMvc;
	@Autowired private JwtEncoder jwtEncoder;
	@MockitoBean private DashboardService dashboardService;

	@Test
	void liderancaAcessaDashboardSemDadosSensiveis() throws Exception {
		when(dashboardService.resumo("usuario-id")).thenReturn(resumo());
		mockMvc.perform(get("/api/dashboard/resumo").header("Authorization", bearer(Role.LIDERANCA)))
				.andExpect(status().isOk()).andExpect(content().string(not(containsString("senhaHash"))))
				.andExpect(content().string(not(containsString("token"))));
	}

	@ParameterizedTest
	@EnumSource(value = Role.class, names = { "GESTOR", "OPERADOR" })
	void gestorEOperadorRecebem403(Role role) throws Exception {
		mockMvc.perform(get("/api/dashboard/resumo").header("Authorization", bearer(role)))
				.andExpect(status().isForbidden());
	}

	@Test
	void semTokenRetorna401() throws Exception {
		mockMvc.perform(get("/api/dashboard/resumo")).andExpect(status().isUnauthorized());
	}

	private String bearer(Role role) {
		Instant agora = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder().issuer("inovagab-api").issuedAt(agora).expiresAt(agora.plusSeconds(3600))
				.subject("usuario-id").claim("role", role.name()).build();
		return "Bearer " + jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
				.getTokenValue();
	}

	private DashboardResumoResponse resumo() {
		return new DashboardResumoResponse(0, 0, 0, 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
				BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0, 0, 0, 0, Map.of(), Map.of());
	}
}
