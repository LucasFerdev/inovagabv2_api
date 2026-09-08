package br.com.inovagab.api.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import br.com.inovagab.api.dto.AtualizarProgressoProjetoRequest;
import br.com.inovagab.api.dto.AtualizarProjetoRequest;
import br.com.inovagab.api.dto.CriarProjetoRequest;
import br.com.inovagab.api.dto.HistoricoProjetoResponse;
import br.com.inovagab.api.dto.PaginaResponse;
import br.com.inovagab.api.dto.ProjetoResponse;
import br.com.inovagab.api.dto.RegistrarResultadosProjetoRequest;
import br.com.inovagab.api.exception.EstrategiaNaoEncontradaException;
import br.com.inovagab.api.exception.IdeiaNaoEncontradaException;
import br.com.inovagab.api.exception.OperacaoProjetoInvalidaException;
import br.com.inovagab.api.exception.ProjetoNaoEncontradoException;
import br.com.inovagab.api.model.AcaoHistoricoProjeto;
import br.com.inovagab.api.model.Estrategia;
import br.com.inovagab.api.model.EtapaProjeto;
import br.com.inovagab.api.model.HistoricoProjeto;
import br.com.inovagab.api.model.Ideia;
import br.com.inovagab.api.model.Projeto;
import br.com.inovagab.api.model.StatusEstrategia;
import br.com.inovagab.api.model.StatusIdeia;
import br.com.inovagab.api.model.StatusProjeto;
import br.com.inovagab.api.repository.EstrategiaRepository;
import br.com.inovagab.api.repository.IdeiaRepository;
import br.com.inovagab.api.repository.ProjetoRepository;

@Service
public class ProjetoService {

	private final ProjetoRepository projetoRepository;
	private final EstrategiaRepository estrategiaRepository;
	private final IdeiaRepository ideiaRepository;
	private final MongoTemplate mongoTemplate;
	private final UsuarioAutenticadoService usuarioAutenticadoService;

	public ProjetoService(ProjetoRepository projetoRepository, EstrategiaRepository estrategiaRepository,
			IdeiaRepository ideiaRepository, MongoTemplate mongoTemplate,
			UsuarioAutenticadoService usuarioAutenticadoService) {
		this.projetoRepository = projetoRepository;
		this.estrategiaRepository = estrategiaRepository;
		this.ideiaRepository = ideiaRepository;
		this.mongoTemplate = mongoTemplate;
		this.usuarioAutenticadoService = usuarioAutenticadoService;
	}

	public ProjetoResponse criar(CriarProjetoRequest request, String usuarioId) {
		usuarioAutenticadoService.buscarAtivo(usuarioId);
		validarReferencias(request.estrategiaId(), request.ideiaOrigemId());
		Projeto projeto = new Projeto();
		aplicarDados(projeto, request.nome(), request.descricao(), request.estrategiaId(), request.ideiaOrigemId(),
				request.investimento(), request.prazo());
		projeto.setEtapa(EtapaProjeto.PLANEJAMENTO);
		projeto.setStatus(StatusProjeto.PLANEJADO);
		projeto.setPercentualProgresso(0);
		projeto.setRetornoFinanceiro(java.math.BigDecimal.ZERO);
		projeto.setGanhoProdutividadePercentual(java.math.BigDecimal.ZERO);
		projeto.setGestorResponsavelId(usuarioId);
		registrarHistorico(projeto, AcaoHistoricoProjeto.CRIADO, usuarioId, null);
		return ProjetoResponse.de(projetoRepository.save(projeto));
	}

	public ProjetoResponse atualizar(String id, AtualizarProjetoRequest request, String usuarioId) {
		usuarioAutenticadoService.buscarAtivo(usuarioId);
		Projeto projeto = buscarEntidade(id);
		validarAberto(projeto);
		validarReferencias(request.estrategiaId(), request.ideiaOrigemId());
		aplicarDados(projeto, request.nome(), request.descricao(), request.estrategiaId(), request.ideiaOrigemId(),
				request.investimento(), request.prazo());
		registrarHistorico(projeto, AcaoHistoricoProjeto.ATUALIZADO, usuarioId, null);
		return ProjetoResponse.de(projetoRepository.save(projeto));
	}

	public ProjetoResponse atualizarProgresso(String id, AtualizarProgressoProjetoRequest request, String usuarioId) {
		usuarioAutenticadoService.buscarAtivo(usuarioId);
		Projeto projeto = buscarEntidade(id);
		validarAberto(projeto);
		if (request.status() == StatusProjeto.CONCLUIDO || request.status() == StatusProjeto.CANCELADO) {
			throw new OperacaoProjetoInvalidaException("Use a operação específica para concluir ou cancelar o projeto");
		}
		if (request.percentualProgresso() < projeto.getPercentualProgresso()
				&& !StringUtils.hasText(request.justificativa())) {
			throw new OperacaoProjetoInvalidaException("A redução do progresso exige justificativa");
		}
		projeto.setEtapa(request.etapa());
		projeto.setStatus(request.status());
		projeto.setPercentualProgresso(request.percentualProgresso());
		registrarHistorico(projeto, AcaoHistoricoProjeto.PROGRESSO_ATUALIZADO, usuarioId, request.justificativa());
		return ProjetoResponse.de(projetoRepository.save(projeto));
	}

	public ProjetoResponse registrarResultados(String id, RegistrarResultadosProjetoRequest request, String usuarioId) {
		usuarioAutenticadoService.buscarAtivo(usuarioId);
		Projeto projeto = buscarEntidade(id);
		validarAberto(projeto);
		projeto.setRetornoFinanceiro(request.retornoFinanceiro());
		projeto.setGanhoProdutividadePercentual(request.ganhoProdutividadePercentual());
		projeto.setResultado(request.resultado());
		registrarHistorico(projeto, AcaoHistoricoProjeto.RESULTADO_REGISTRADO, usuarioId, null);
		return ProjetoResponse.de(projetoRepository.save(projeto));
	}

	public ProjetoResponse concluir(String id, String usuarioId) {
		usuarioAutenticadoService.buscarAtivo(usuarioId);
		Projeto projeto = buscarEntidade(id);
		validarAberto(projeto);
		projeto.setStatus(StatusProjeto.CONCLUIDO);
		projeto.setEtapa(EtapaProjeto.ENCERRAMENTO);
		projeto.setPercentualProgresso(100);
		registrarHistorico(projeto, AcaoHistoricoProjeto.CONCLUIDO, usuarioId, null);
		return ProjetoResponse.de(projetoRepository.save(projeto));
	}

	public void cancelar(String id, String usuarioId) {
		usuarioAutenticadoService.buscarAtivo(usuarioId);
		Projeto projeto = buscarEntidade(id);
		validarAberto(projeto);
		projeto.setStatus(StatusProjeto.CANCELADO);
		registrarHistorico(projeto, AcaoHistoricoProjeto.CANCELADO, usuarioId, null);
		projetoRepository.save(projeto);
	}

	public ProjetoResponse buscarPorId(String id, String usuarioId) {
		usuarioAutenticadoService.buscarAtivo(usuarioId);
		return ProjetoResponse.de(buscarEntidade(id));
	}

	public List<HistoricoProjetoResponse> consultarHistorico(String id, String usuarioId) {
		usuarioAutenticadoService.buscarAtivo(usuarioId);
		return buscarEntidade(id).getHistorico().stream()
				.sorted(Comparator.comparing(HistoricoProjeto::getDataHora).reversed())
				.map(HistoricoProjetoResponse::de).toList();
	}

	public PaginaResponse<ProjetoResponse> listar(StatusProjeto status, EtapaProjeto etapa, String estrategiaId,
			String gestorId, LocalDate prazo, int pagina, int tamanho, String usuarioId) {
		usuarioAutenticadoService.buscarAtivo(usuarioId);
		Query query = new Query();
		if (status != null) query.addCriteria(Criteria.where("status").is(status));
		if (etapa != null) query.addCriteria(Criteria.where("etapa").is(etapa));
		if (StringUtils.hasText(estrategiaId)) query.addCriteria(Criteria.where("estrategiaId").is(estrategiaId.strip()));
		if (StringUtils.hasText(gestorId)) query.addCriteria(Criteria.where("gestorResponsavelId").is(gestorId.strip()));
		if (prazo != null) query.addCriteria(Criteria.where("prazo").is(prazo));
		long total = mongoTemplate.count(query, Projeto.class);
		PageRequest pageable = PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.DESC, "criadoEm"));
		List<ProjetoResponse> conteudo = mongoTemplate.find(query.with(pageable), Projeto.class).stream()
				.map(ProjetoResponse::de).toList();
		return PaginaResponse.de(conteudo, pagina, tamanho, total);
	}

	private void validarReferencias(String estrategiaId, String ideiaOrigemId) {
		Estrategia estrategia = estrategiaRepository.findById(estrategiaId)
				.orElseThrow(EstrategiaNaoEncontradaException::new);
		if (estrategia.getStatus() != StatusEstrategia.ATIVA) {
			throw new OperacaoProjetoInvalidaException("A estratégia deve estar ativa");
		}
		if (ideiaOrigemId != null) {
			Ideia ideia = ideiaRepository.findById(ideiaOrigemId).orElseThrow(IdeiaNaoEncontradaException::new);
			if (ideia.getStatus() != StatusIdeia.APROVADA) {
				throw new OperacaoProjetoInvalidaException("A ideia de origem deve estar aprovada");
			}
			if (!estrategiaId.equals(ideia.getEstrategiaId())) {
				throw new OperacaoProjetoInvalidaException("A ideia e o projeto devem utilizar a mesma estratégia");
			}
		}
	}

	private void validarAberto(Projeto projeto) {
		if (projeto.getStatus() == StatusProjeto.CONCLUIDO || projeto.getStatus() == StatusProjeto.CANCELADO) {
			throw new OperacaoProjetoInvalidaException("Projeto concluído ou cancelado não pode ser alterado");
		}
	}

	private Projeto buscarEntidade(String id) {
		return projetoRepository.findById(id).orElseThrow(ProjetoNaoEncontradoException::new);
	}

	private void aplicarDados(Projeto projeto, String nome, String descricao, String estrategiaId,
			String ideiaOrigemId, java.math.BigDecimal investimento, LocalDate prazo) {
		projeto.setNome(nome); projeto.setDescricao(descricao); projeto.setEstrategiaId(estrategiaId);
		projeto.setIdeiaOrigemId(ideiaOrigemId); projeto.setInvestimento(investimento); projeto.setPrazo(prazo);
	}

	private void registrarHistorico(Projeto projeto, AcaoHistoricoProjeto acao, String usuarioId,
			String justificativa) {
		projeto.adicionarHistorico(new HistoricoProjeto(UUID.randomUUID().toString(), acao, Instant.now(), usuarioId,
				projeto.getNome(), projeto.getDescricao(), projeto.getEstrategiaId(), projeto.getIdeiaOrigemId(),
				projeto.getEtapa(), projeto.getStatus(), projeto.getPercentualProgresso(), projeto.getInvestimento(),
				projeto.getPrazo(), projeto.getRetornoFinanceiro(), projeto.getGanhoProdutividadePercentual(),
				projeto.getResultado(), justificativa));
	}
}
