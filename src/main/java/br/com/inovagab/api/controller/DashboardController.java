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

@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasRole('LIDERANCA')")
public class DashboardController {

	private final DashboardService dashboardService;

	public DashboardController(DashboardService dashboardService) {
		this.dashboardService = dashboardService;
	}

	@GetMapping("/resumo")
	public DashboardResumoResponse resumo(@AuthenticationPrincipal Jwt jwt) {
		return dashboardService.resumo(jwt.getSubject());
	}

	@GetMapping("/estrategias/{estrategiaId}")
	public DashboardEstrategiaResponse porEstrategia(@PathVariable String estrategiaId,
			@AuthenticationPrincipal Jwt jwt) {
		return dashboardService.porEstrategia(estrategiaId, jwt.getSubject());
	}

	@GetMapping("/projetos/{projetoId}")
	public DashboardProjetoResponse porProjeto(@PathVariable String projetoId, @AuthenticationPrincipal Jwt jwt) {
		return dashboardService.porProjeto(projetoId, jwt.getSubject());
	}
}
