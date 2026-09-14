package br.com.inovagab.api.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.inovagab.api.dto.RankingColaboradorResponse;
import br.com.inovagab.api.service.RankingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/ranking")
@PreAuthorize("hasAnyRole('OPERADOR', 'GESTOR', 'LIDERANCA')")
@Tag(name = "Ranking", description = "Ranking de colaboradores por ideias implementadas.")
public class RankingController {

	private final RankingService rankingService;

	public RankingController(RankingService rankingService) {
		this.rankingService = rankingService;
	}

	@GetMapping("/colaboradores")
	@Operation(summary = "Consultar ranking de colaboradores")
	public List<RankingColaboradorResponse> colaboradores(@AuthenticationPrincipal Jwt jwt) {
		return rankingService.colaboradores(jwt.getSubject());
	}
}
