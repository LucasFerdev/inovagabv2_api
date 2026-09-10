package br.com.inovagab.api.controller;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.inovagab.api.dto.AtualizarEstrategiaRequest;
import br.com.inovagab.api.dto.CriarEstrategiaRequest;
import br.com.inovagab.api.dto.EstrategiaResponse;
import br.com.inovagab.api.dto.HistoricoEstrategiaResponse;
import br.com.inovagab.api.dto.PaginaResponse;
import br.com.inovagab.api.model.StatusEstrategia;
import br.com.inovagab.api.service.EstrategiaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Validated
@RestController
@RequestMapping("/api/estrategias")
@Tag(name = "Estratégias", description = "Gestão, consulta, ciclo de vida e histórico das estratégias.")
public class EstrategiaController {

	private static final String PODE_CONSULTAR = "hasAnyRole('OPERADOR', 'GESTOR', 'LIDERANCA')";
	private static final String PODE_ALTERAR = "hasRole('LIDERANCA')";

	private final EstrategiaService estrategiaService;

	public EstrategiaController(EstrategiaService estrategiaService) {
		this.estrategiaService = estrategiaService;
	}

	@PostMapping
	@Operation(summary = "Criar estratégia")
	@PreAuthorize(PODE_ALTERAR)
	public ResponseEntity<EstrategiaResponse> criar(@Valid @RequestBody CriarEstrategiaRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.status(HttpStatus.CREATED).body(estrategiaService.criar(request, jwt.getSubject()));
	}

	@GetMapping
	@Operation(summary = "Listar estratégias com filtros e paginação")
	@PreAuthorize(PODE_CONSULTAR)
	public PaginaResponse<EstrategiaResponse> listar(
			@RequestParam(required = false) StatusEstrategia status,
			@RequestParam(required = false) String categoria,
			@RequestParam(required = false) String campanha,
			@RequestParam(defaultValue = "0") @Min(value = 0, message = "A página não pode ser negativa") int pagina,
			@RequestParam(defaultValue = "20") @Min(value = 1, message = "O tamanho deve ser positivo") @Max(value = 100, message = "O tamanho máximo é 100") int tamanho,
			@AuthenticationPrincipal Jwt jwt) {
		return estrategiaService.listar(status, categoria, campanha, pagina, tamanho, jwt.getSubject());
	}

	@GetMapping("/ativas")
	@Operation(summary = "Listar estratégias ativas")
	@PreAuthorize(PODE_CONSULTAR)
	public List<EstrategiaResponse> listarAtivas(@AuthenticationPrincipal Jwt jwt) {
		return estrategiaService.listarAtivas(jwt.getSubject());
	}

	@GetMapping("/{id}")
	@Operation(summary = "Consultar estratégia por ID")
	@PreAuthorize(PODE_CONSULTAR)
	public EstrategiaResponse buscarPorId(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
		return estrategiaService.buscarPorId(id, jwt.getSubject());
	}

	@GetMapping("/{id}/historico")
	@Operation(summary = "Consultar histórico da estratégia")
	@PreAuthorize(PODE_CONSULTAR)
	public List<HistoricoEstrategiaResponse> consultarHistorico(@PathVariable String id,
			@AuthenticationPrincipal Jwt jwt) {
		return estrategiaService.consultarHistorico(id, jwt.getSubject());
	}

	@PutMapping("/{id}")
	@Operation(summary = "Atualizar estratégia")
	@PreAuthorize(PODE_ALTERAR)
	public EstrategiaResponse atualizar(@PathVariable String id,
			@Valid @RequestBody AtualizarEstrategiaRequest request, @AuthenticationPrincipal Jwt jwt) {
		return estrategiaService.atualizar(id, request, jwt.getSubject());
	}

	@PatchMapping("/{id}/ativar")
	@Operation(summary = "Ativar estratégia")
	@PreAuthorize(PODE_ALTERAR)
	public EstrategiaResponse ativar(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
		return estrategiaService.ativar(id, jwt.getSubject());
	}

	@PatchMapping("/{id}/desativar")
	@Operation(summary = "Desativar estratégia")
	@PreAuthorize(PODE_ALTERAR)
	public EstrategiaResponse desativar(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
		return estrategiaService.desativar(id, jwt.getSubject());
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Arquivar estratégia")
	@PreAuthorize(PODE_ALTERAR)
	public ResponseEntity<Void> arquivar(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
		estrategiaService.arquivar(id, jwt.getSubject());
		return ResponseEntity.noContent().build();
	}
}
