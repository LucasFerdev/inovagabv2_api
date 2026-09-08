package br.com.inovagab.api.controller;

import java.time.LocalDate;
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

import br.com.inovagab.api.dto.AtualizarProgressoProjetoRequest;
import br.com.inovagab.api.dto.AtualizarProjetoRequest;
import br.com.inovagab.api.dto.CriarProjetoRequest;
import br.com.inovagab.api.dto.HistoricoProjetoResponse;
import br.com.inovagab.api.dto.PaginaResponse;
import br.com.inovagab.api.dto.ProjetoResponse;
import br.com.inovagab.api.dto.RegistrarResultadosProjetoRequest;
import br.com.inovagab.api.model.EtapaProjeto;
import br.com.inovagab.api.model.StatusProjeto;
import br.com.inovagab.api.service.ProjetoService;

@Validated
@RestController
@RequestMapping("/api/projetos")
public class ProjetoController {

	private static final String CONSULTA = "hasAnyRole('GESTOR', 'LIDERANCA')";
	private static final String ESCRITA = "hasRole('GESTOR')";
	private final ProjetoService projetoService;

	public ProjetoController(ProjetoService projetoService) {
		this.projetoService = projetoService;
	}

	@PostMapping
	@PreAuthorize(ESCRITA)
	public ResponseEntity<ProjetoResponse> criar(@Valid @RequestBody CriarProjetoRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.status(HttpStatus.CREATED).body(projetoService.criar(request, jwt.getSubject()));
	}

	@GetMapping
	@PreAuthorize(CONSULTA)
	public PaginaResponse<ProjetoResponse> listar(@RequestParam(required = false) StatusProjeto status,
			@RequestParam(required = false) EtapaProjeto etapa,
			@RequestParam(required = false) String estrategiaId,
			@RequestParam(required = false) String gestorId,
			@RequestParam(required = false) LocalDate prazo,
			@RequestParam(defaultValue = "0") @Min(0) int pagina,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanho,
			@AuthenticationPrincipal Jwt jwt) {
		return projetoService.listar(status, etapa, estrategiaId, gestorId, prazo, pagina, tamanho, jwt.getSubject());
	}

	@GetMapping("/{id}")
	@PreAuthorize(CONSULTA)
	public ProjetoResponse buscar(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
		return projetoService.buscarPorId(id, jwt.getSubject());
	}

	@GetMapping("/{id}/historico")
	@PreAuthorize(CONSULTA)
	public List<HistoricoProjetoResponse> historico(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
		return projetoService.consultarHistorico(id, jwt.getSubject());
	}

	@PutMapping("/{id}")
	@PreAuthorize(ESCRITA)
	public ProjetoResponse atualizar(@PathVariable String id, @Valid @RequestBody AtualizarProjetoRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		return projetoService.atualizar(id, request, jwt.getSubject());
	}

	@PatchMapping("/{id}/progresso")
	@PreAuthorize(ESCRITA)
	public ProjetoResponse atualizarProgresso(@PathVariable String id,
			@Valid @RequestBody AtualizarProgressoProjetoRequest request, @AuthenticationPrincipal Jwt jwt) {
		return projetoService.atualizarProgresso(id, request, jwt.getSubject());
	}

	@PatchMapping("/{id}/resultados")
	@PreAuthorize(ESCRITA)
	public ProjetoResponse registrarResultados(@PathVariable String id,
			@Valid @RequestBody RegistrarResultadosProjetoRequest request, @AuthenticationPrincipal Jwt jwt) {
		return projetoService.registrarResultados(id, request, jwt.getSubject());
	}

	@PatchMapping("/{id}/concluir")
	@PreAuthorize(ESCRITA)
	public ProjetoResponse concluir(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
		return projetoService.concluir(id, jwt.getSubject());
	}

	@DeleteMapping("/{id}")
	@PreAuthorize(ESCRITA)
	public ResponseEntity<Void> cancelar(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
		projetoService.cancelar(id, jwt.getSubject());
		return ResponseEntity.noContent().build();
	}
}
