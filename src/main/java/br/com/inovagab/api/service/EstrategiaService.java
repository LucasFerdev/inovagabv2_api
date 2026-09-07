package br.com.inovagab.api.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import br.com.inovagab.api.dto.AtualizarEstrategiaRequest;
import br.com.inovagab.api.dto.CriarEstrategiaRequest;
import br.com.inovagab.api.dto.EstrategiaResponse;
import br.com.inovagab.api.dto.HistoricoEstrategiaResponse;
import br.com.inovagab.api.dto.PaginaResponse;
import br.com.inovagab.api.exception.EstrategiaNaoEncontradaException;
import br.com.inovagab.api.exception.OperacaoEstrategiaInvalidaException;
import br.com.inovagab.api.model.AcaoHistoricoEstrategia;
import br.com.inovagab.api.model.Estrategia;
import br.com.inovagab.api.model.HistoricoEstrategia;
import br.com.inovagab.api.model.StatusEstrategia;
import br.com.inovagab.api.repository.EstrategiaRepository;

@Service
public class EstrategiaService {

	private final EstrategiaRepository estrategiaRepository;
	private final MongoTemplate mongoTemplate;
	private final UsuarioAutenticadoService usuarioAutenticadoService;

	public EstrategiaService(EstrategiaRepository estrategiaRepository, MongoTemplate mongoTemplate,
			UsuarioAutenticadoService usuarioAutenticadoService) {
		this.estrategiaRepository = estrategiaRepository;
		this.mongoTemplate = mongoTemplate;
		this.usuarioAutenticadoService = usuarioAutenticadoService;
	}

	public EstrategiaResponse criar(CriarEstrategiaRequest request, String usuarioId) {
		usuarioAutenticadoService.buscarAtivo(usuarioId);
		Estrategia estrategia = new Estrategia();
		aplicarDados(estrategia, request.titulo(), request.descricao(), request.data(), request.categoria(),
				request.campanha());
		estrategia.setStatus(StatusEstrategia.RASCUNHO);
		estrategia.setCriadoPorId(usuarioId);
		estrategia.setAtualizadoPorId(usuarioId);
		registrarHistorico(estrategia, AcaoHistoricoEstrategia.CRIADA, usuarioId);
		return EstrategiaResponse.de(estrategiaRepository.save(estrategia));
	}

	public EstrategiaResponse atualizar(String id, AtualizarEstrategiaRequest request, String usuarioId) {
		usuarioAutenticadoService.buscarAtivo(usuarioId);
		Estrategia estrategia = buscarEntidade(id);
		validarNaoArquivada(estrategia);
		aplicarDados(estrategia, request.titulo(), request.descricao(), request.data(), request.categoria(),
				request.campanha());
		estrategia.setAtualizadoPorId(usuarioId);
		registrarHistorico(estrategia, AcaoHistoricoEstrategia.ATUALIZADA, usuarioId);
		return EstrategiaResponse.de(estrategiaRepository.save(estrategia));
	}

	public EstrategiaResponse ativar(String id, String usuarioId) {
		usuarioAutenticadoService.buscarAtivo(usuarioId);
		Estrategia estrategia = buscarEntidade(id);
		validarNaoArquivada(estrategia);
		if (estrategia.getStatus() == StatusEstrategia.ATIVA) {
			return EstrategiaResponse.de(estrategia);
		}
		estrategia.setStatus(StatusEstrategia.ATIVA);
		estrategia.setAtualizadoPorId(usuarioId);
		registrarHistorico(estrategia, AcaoHistoricoEstrategia.ATIVADA, usuarioId);
		return EstrategiaResponse.de(estrategiaRepository.save(estrategia));
	}

	public EstrategiaResponse desativar(String id, String usuarioId) {
		usuarioAutenticadoService.buscarAtivo(usuarioId);
		Estrategia estrategia = buscarEntidade(id);
		validarNaoArquivada(estrategia);
		if (estrategia.getStatus() == StatusEstrategia.INATIVA) {
			return EstrategiaResponse.de(estrategia);
		}
		if (estrategia.getStatus() != StatusEstrategia.ATIVA) {
			throw new OperacaoEstrategiaInvalidaException("Somente uma estratégia ativa pode ser desativada");
		}
		estrategia.setStatus(StatusEstrategia.INATIVA);
		estrategia.setAtualizadoPorId(usuarioId);
		registrarHistorico(estrategia, AcaoHistoricoEstrategia.DESATIVADA, usuarioId);
		return EstrategiaResponse.de(estrategiaRepository.save(estrategia));
	}

	public void arquivar(String id, String usuarioId) {
		usuarioAutenticadoService.buscarAtivo(usuarioId);
		Estrategia estrategia = buscarEntidade(id);
		validarNaoArquivada(estrategia);
		estrategia.setStatus(StatusEstrategia.ARQUIVADA);
		estrategia.setAtualizadoPorId(usuarioId);
		registrarHistorico(estrategia, AcaoHistoricoEstrategia.ARQUIVADA, usuarioId);
		estrategiaRepository.save(estrategia);
	}

	public EstrategiaResponse buscarPorId(String id, String usuarioId) {
		usuarioAutenticadoService.buscarAtivo(usuarioId);
		return EstrategiaResponse.de(buscarEntidade(id));
	}

	public PaginaResponse<EstrategiaResponse> listar(StatusEstrategia status, String categoria, String campanha,
			int pagina, int tamanho, String usuarioId) {
		usuarioAutenticadoService.buscarAtivo(usuarioId);
		Query query = criarConsulta(status, categoria, campanha);
		long total = mongoTemplate.count(query, Estrategia.class);
		PageRequest paginacao = PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.DESC, "data"));
		List<EstrategiaResponse> conteudo = mongoTemplate.find(query.with(paginacao), Estrategia.class).stream()
				.map(EstrategiaResponse::de)
				.toList();
		return PaginaResponse.de(conteudo, pagina, tamanho, total);
	}

	public List<EstrategiaResponse> listarAtivas(String usuarioId) {
		usuarioAutenticadoService.buscarAtivo(usuarioId);
		return estrategiaRepository.findByStatusOrderByDataDesc(StatusEstrategia.ATIVA).stream()
				.map(EstrategiaResponse::de)
				.toList();
	}

	public List<HistoricoEstrategiaResponse> consultarHistorico(String id, String usuarioId) {
		usuarioAutenticadoService.buscarAtivo(usuarioId);
		return buscarEntidade(id).getHistorico().stream()
				.sorted(Comparator.comparing(HistoricoEstrategia::getDataHora).reversed())
				.map(HistoricoEstrategiaResponse::de)
				.toList();
	}

	private Query criarConsulta(StatusEstrategia status, String categoria, String campanha) {
		Query query = new Query();
		if (status != null) {
			query.addCriteria(Criteria.where("status").is(status));
		}
		if (StringUtils.hasText(categoria)) {
			query.addCriteria(Criteria.where("categoria").regex(expressaoExata(categoria), "i"));
		}
		if (StringUtils.hasText(campanha)) {
			query.addCriteria(Criteria.where("campanha").regex(expressaoExata(campanha), "i"));
		}
		return query;
	}

	private String expressaoExata(String valor) {
		return "^" + Pattern.quote(valor.strip()) + "$";
	}

	private Estrategia buscarEntidade(String id) {
		return estrategiaRepository.findById(id).orElseThrow(EstrategiaNaoEncontradaException::new);
	}

	private void validarNaoArquivada(Estrategia estrategia) {
		if (estrategia.getStatus() == StatusEstrategia.ARQUIVADA) {
			throw new OperacaoEstrategiaInvalidaException("Estratégia arquivada não pode ser alterada");
		}
	}

	private void aplicarDados(Estrategia estrategia, String titulo, String descricao, LocalDate data,
			String categoria, String campanha) {
		estrategia.setTitulo(titulo.strip());
		estrategia.setDescricao(descricao.strip());
		estrategia.setData(data);
		estrategia.setCategoria(categoria.strip());
		estrategia.setCampanha(campanha.strip());
	}

	private void registrarHistorico(Estrategia estrategia, AcaoHistoricoEstrategia acao, String usuarioId) {
		estrategia.adicionarHistorico(new HistoricoEstrategia(
				UUID.randomUUID().toString(),
				acao,
				Instant.now(),
				usuarioId,
				estrategia.getTitulo(),
				estrategia.getDescricao(),
				estrategia.getData(),
				estrategia.getCategoria(),
				estrategia.getCampanha(),
				estrategia.getStatus()));
	}
}
