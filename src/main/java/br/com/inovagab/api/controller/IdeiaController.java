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

import br.com.inovagab.api.dto.AtualizarIdeiaRequest;
import br.com.inovagab.api.dto.CriarIdeiaRequest;
import br.com.inovagab.api.dto.HistoricoIdeiaResponse;
import br.com.inovagab.api.dto.IdeiaResponse;
import br.com.inovagab.api.dto.PaginaResponse;
import br.com.inovagab.api.dto.PriorizarIdeiaRequest;
import br.com.inovagab.api.dto.RejeitarIdeiaRequest;
import br.com.inovagab.api.model.StatusIdeia;
import br.com.inovagab.api.service.IdeiaService;

@Validated
@RestController
@RequestMapping("/api/ideias")
public class IdeiaController {

	private final IdeiaService ideiaService;

	public IdeiaController(IdeiaService ideiaService) {
		this.ideiaService = ideiaService;
	}

	@PostMapping
	@PreAuthorize("hasRole('OPERADOR')")
	public ResponseEntity<IdeiaResponse> criar(@Valid @RequestBody CriarIdeiaRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.status(HttpStatus.CREATED).body(ideiaService.criar(request, jwt.getSubject()));
	}

	@GetMapping("/minhas")
	@PreAuthorize("hasRole('OPERADOR')")
	public PaginaResponse<IdeiaResponse> listarMinhas(
			@RequestParam(defaultValue = "0") @Min(0) int pagina,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanho,
			@AuthenticationPrincipal Jwt jwt) {
		return ideiaService.listarMinhas(pagina, tamanho, jwt.getSubject());
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('GESTOR', 'LIDERANCA')")
	public PaginaResponse<IdeiaResponse> listar(@RequestParam(required = false) StatusIdeia status,
			@RequestParam(required = false) String categoria,
			@RequestParam(required = false) String estrategiaId,
			@RequestParam(required = false) @Min(1) @Max(5) Integer prioridade,
			@RequestParam(defaultValue = "0") @Min(0) int pagina,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanho,
			@AuthenticationPrincipal Jwt jwt) {
		return ideiaService.listar(status, categoria, estrategiaId, prioridade, pagina, tamanho, jwt.getSubject());
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('OPERADOR', 'GESTOR', 'LIDERANCA')")
	public IdeiaResponse buscar(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
		return ideiaService.buscarPorId(id, jwt.getSubject());
	}

	@GetMapping("/{id}/historico")
	@PreAuthorize("hasAnyRole('OPERADOR', 'GESTOR', 'LIDERANCA')")
	public List<HistoricoIdeiaResponse> historico(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
		return ideiaService.consultarHistorico(id, jwt.getSubject());
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('OPERADOR')")
	public IdeiaResponse atualizar(@PathVariable String id, @Valid @RequestBody AtualizarIdeiaRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		return ideiaService.atualizar(id, request, jwt.getSubject());
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('OPERADOR')")
	public ResponseEntity<Void> arquivar(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
		ideiaService.arquivar(id, jwt.getSubject());
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/{id}/analisar")
	@PreAuthorize("hasRole('GESTOR')")
	public IdeiaResponse analisar(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
		return ideiaService.analisar(id, jwt.getSubject());
	}

	@PatchMapping("/{id}/priorizar")
	@PreAuthorize("hasRole('GESTOR')")
	public IdeiaResponse priorizar(@PathVariable String id, @Valid @RequestBody PriorizarIdeiaRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		return ideiaService.priorizar(id, request, jwt.getSubject());
	}

	@PatchMapping("/{id}/aprovar")
	@PreAuthorize("hasRole('GESTOR')")
	public IdeiaResponse aprovar(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
		return ideiaService.aprovar(id, jwt.getSubject());
	}

	@PatchMapping("/{id}/rejeitar")
	@PreAuthorize("hasRole('GESTOR')")
	public IdeiaResponse rejeitar(@PathVariable String id, @Valid @RequestBody RejeitarIdeiaRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		return ideiaService.rejeitar(id, request, jwt.getSubject());
	}
}
