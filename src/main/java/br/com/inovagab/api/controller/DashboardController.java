package br.com.inovagab.api.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.inovagab.api.dto.DashboardEstrategiaResponse;
import br.com.inovagab.api.dto.DashboardProjetoResponse;
import br.com.inovagab.api.dto.DashboardResumoResponse;
import br.com.inovagab.api.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasRole('LIDERANCA')")
@Tag(name = "Dashboard", description = "Indicadores executivos consolidados para a liderança.")
public class DashboardController {

	private final DashboardService dashboardService;

	public DashboardController(DashboardService dashboardService) {
		this.dashboardService = dashboardService;
	}

	@GetMapping("/resumo")
	@Operation(summary = "Consultar resumo executivo")
	public DashboardResumoResponse resumo(@AuthenticationPrincipal Jwt jwt) {
		return dashboardService.resumo(jwt.getSubject());
	}

	@GetMapping("/estrategias/{estrategiaId}")
	@Operation(summary = "Consultar indicadores de uma estratégia")
	public DashboardEstrategiaResponse porEstrategia(@PathVariable String estrategiaId,
			@AuthenticationPrincipal Jwt jwt) {
		return dashboardService.porEstrategia(estrategiaId, jwt.getSubject());
	}

	@GetMapping("/projetos/{projetoId}")
	@Operation(summary = "Consultar indicadores detalhados de um projeto")
	public DashboardProjetoResponse porProjeto(@PathVariable String projetoId, @AuthenticationPrincipal Jwt jwt) {
		return dashboardService.porProjeto(projetoId, jwt.getSubject());
	}
}
