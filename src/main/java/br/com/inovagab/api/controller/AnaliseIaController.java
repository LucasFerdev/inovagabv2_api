package br.com.inovagab.api.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.inovagab.api.dto.AnaliseIaIdeiaResponse;
import br.com.inovagab.api.service.AnaliseIaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/ia/ideias")
@Tag(name = "Inteligência Artificial", description = "Análise consultiva de ideias utilizando Gemini.")
public class AnaliseIaController {

	private final AnaliseIaService analiseIaService;

	public AnaliseIaController(AnaliseIaService analiseIaService) {
		this.analiseIaService = analiseIaService;
	}

	@PostMapping("/{ideiaId}/analisar")
	@Operation(summary = "Analisar ideia com Gemini", description = "Gera ou recalcula uma análise consultiva sem alterar a decisão humana.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Análise gerada ou recuperada"),
			@ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
			@ApiResponse(responseCode = "403", description = "Perfil sem permissão"),
			@ApiResponse(responseCode = "404", description = "Ideia não encontrada"),
			@ApiResponse(responseCode = "409", description = "Status incompatível"),
			@ApiResponse(responseCode = "429", description = "Limite do provedor atingido"),
			@ApiResponse(responseCode = "502", description = "Resposta inválida ou provedor indisponível"),
			@ApiResponse(responseCode = "503", description = "Integração não configurada"),
			@ApiResponse(responseCode = "504", description = "Timeout do provedor") })
	@PreAuthorize("hasRole('GESTOR')")
	public AnaliseIaIdeiaResponse analisar(@PathVariable String ideiaId,
			@RequestParam(defaultValue = "false") boolean recalcular, @AuthenticationPrincipal Jwt jwt) {
		return analiseIaService.analisar(ideiaId, recalcular, jwt.getSubject());
	}

	@GetMapping("/{ideiaId}/analise")
	@Operation(summary = "Consultar análise armazenada", description = "Retorna a análise persistida sem chamar novamente o Gemini.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Análise encontrada"),
			@ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
			@ApiResponse(responseCode = "403", description = "Perfil sem permissão"),
			@ApiResponse(responseCode = "404", description = "Ideia ou análise não encontrada") })
	@PreAuthorize("hasAnyRole('GESTOR', 'LIDERANCA')")
	public AnaliseIaIdeiaResponse consultar(@PathVariable String ideiaId, @AuthenticationPrincipal Jwt jwt) {
		return analiseIaService.consultar(ideiaId, jwt.getSubject());
	}
}
