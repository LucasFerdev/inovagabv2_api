package br.com.inovagab.api.service;

import java.time.Instant;
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

import br.com.inovagab.api.dto.AtualizarIdeiaRequest;
import br.com.inovagab.api.dto.CriarIdeiaRequest;
import br.com.inovagab.api.dto.HistoricoIdeiaResponse;
import br.com.inovagab.api.dto.IdeiaResponse;
import br.com.inovagab.api.dto.PaginaResponse;
import br.com.inovagab.api.dto.PriorizarIdeiaRequest;
import br.com.inovagab.api.dto.RejeitarIdeiaRequest;
import br.com.inovagab.api.exception.EstrategiaNaoEncontradaException;
import br.com.inovagab.api.exception.IdeiaNaoEncontradaException;
import br.com.inovagab.api.exception.OperacaoIdeiaInvalidaException;
import br.com.inovagab.api.model.AcaoHistoricoIdeia;
import br.com.inovagab.api.model.Estrategia;
import br.com.inovagab.api.model.HistoricoIdeia;
import br.com.inovagab.api.model.Ideia;
import br.com.inovagab.api.model.Role;
import br.com.inovagab.api.model.StatusEstrategia;
import br.com.inovagab.api.model.StatusIdeia;
import br.com.inovagab.api.model.Usuario;
import br.com.inovagab.api.repository.EstrategiaRepository;
import br.com.inovagab.api.repository.IdeiaRepository;

@Service
public class IdeiaService {

	private final IdeiaRepository ideiaRepository;
	private final EstrategiaRepository estrategiaRepository;
	private final MongoTemplate mongoTemplate;
	private final UsuarioAutenticadoService usuarioAutenticadoService;

	public IdeiaService(IdeiaRepository ideiaRepository, EstrategiaRepository estrategiaRepository,
			MongoTemplate mongoTemplate, UsuarioAutenticadoService usuarioAutenticadoService) {
		this.ideiaRepository = ideiaRepository;
		this.estrategiaRepository = estrategiaRepository;
		this.mongoTemplate = mongoTemplate;
		this.usuarioAutenticadoService = usuarioAutenticadoService;
	}

	public IdeiaResponse criar(CriarIdeiaRequest request, String usuarioId) {
		usuarioAutenticadoService.buscarAtivo(usuarioId);
		validarEstrategiaAtiva(request.estrategiaId());
		Ideia ideia = new Ideia();
		aplicarDados(ideia, request.titulo(), request.problema(), request.solucaoProposta(),
				request.beneficiosEsperados(), request.categoria(), request.estrategiaId());
		ideia.setAutorId(usuarioId);
		ideia.setStatus(StatusIdeia.ENVIADA);
		registrarHistorico(ideia, AcaoHistoricoIdeia.CRIADA, usuarioId);
		return IdeiaResponse.de(ideiaRepository.save(ideia));
	}

	public IdeiaResponse atualizar(String id, AtualizarIdeiaRequest request, String usuarioId) {
		usuarioAutenticadoService.buscarAtivo(usuarioId);
		Ideia ideia = buscarEntidade(id);
		validarAutorEEnviada(ideia, usuarioId);
		validarEstrategiaAtiva(request.estrategiaId());
		aplicarDados(ideia, request.titulo(), request.problema(), request.solucaoProposta(),
				request.beneficiosEsperados(), request.categoria(), request.estrategiaId());
		registrarHistorico(ideia, AcaoHistoricoIdeia.ATUALIZADA, usuarioId);
		return IdeiaResponse.de(ideiaRepository.save(ideia));
	}

	public void arquivar(String id, String usuarioId) {
		usuarioAutenticadoService.buscarAtivo(usuarioId);
		Ideia ideia = buscarEntidade(id);
		validarAutorEEnviada(ideia, usuarioId);
		ideia.setStatus(StatusIdeia.ARQUIVADA);
		registrarHistorico(ideia, AcaoHistoricoIdeia.ARQUIVADA, usuarioId);
		ideiaRepository.save(ideia);
	}

	public IdeiaResponse analisar(String id, String usuarioId) {
		usuarioAutenticadoService.buscarAtivo(usuarioId);
		Ideia ideia = buscarEntidade(id);
		validarStatus(ideia, StatusIdeia.ENVIADA, "Somente uma ideia enviada pode entrar em análise");
		ideia.setStatus(StatusIdeia.EM_ANALISE);
		registrarHistorico(ideia, AcaoHistoricoIdeia.ENVIADA_PARA_ANALISE, usuarioId);
		return IdeiaResponse.de(ideiaRepository.save(ideia));
	}

	public IdeiaResponse priorizar(String id, PriorizarIdeiaRequest request, String usuarioId) {
		usuarioAutenticadoService.buscarAtivo(usuarioId);
		Ideia ideia = buscarEntidade(id);
		validarStatus(ideia, StatusIdeia.EM_ANALISE, "Somente uma ideia em análise pode ser priorizada");
		ideia.setPrioridade(request.prioridade());
		ideia.setJustificativaAvaliacao(request.justificativa());
		ideia.setAvaliadoPorId(usuarioId);
		ideia.setAvaliadoEm(Instant.now());
		registrarHistorico(ideia, AcaoHistoricoIdeia.PRIORIZADA, usuarioId);
		return IdeiaResponse.de(ideiaRepository.save(ideia));
	}

	public IdeiaResponse aprovar(String id, String usuarioId) {
		usuarioAutenticadoService.buscarAtivo(usuarioId);
		Ideia ideia = buscarEntidade(id);
		validarStatus(ideia, StatusIdeia.EM_ANALISE, "Somente uma ideia em análise pode ser aprovada");
		if (ideia.getPrioridade() == null) {
			throw new OperacaoIdeiaInvalidaException("Defina a prioridade antes de aprovar a ideia");
		}
		ideia.setStatus(StatusIdeia.APROVADA);
		ideia.setAvaliadoPorId(usuarioId);
		ideia.setAvaliadoEm(Instant.now());
		registrarHistorico(ideia, AcaoHistoricoIdeia.APROVADA, usuarioId);
		return IdeiaResponse.de(ideiaRepository.save(ideia));
	}

	public IdeiaResponse rejeitar(String id, RejeitarIdeiaRequest request, String usuarioId) {
		usuarioAutenticadoService.buscarAtivo(usuarioId);
		Ideia ideia = buscarEntidade(id);
		validarStatus(ideia, StatusIdeia.EM_ANALISE, "Somente uma ideia em análise pode ser rejeitada");
		ideia.setStatus(StatusIdeia.REJEITADA);
		ideia.setJustificativaAvaliacao(request.justificativa());
		ideia.setAvaliadoPorId(usuarioId);
		ideia.setAvaliadoEm(Instant.now());
		registrarHistorico(ideia, AcaoHistoricoIdeia.REJEITADA, usuarioId);
		return IdeiaResponse.de(ideiaRepository.save(ideia));
	}

	public IdeiaResponse buscarPorId(String id, String usuarioId) {
		Usuario usuario = usuarioAutenticadoService.buscarAtivo(usuarioId);
		Ideia ideia = buscarEntidade(id);
		validarConsulta(ideia, usuario);
		return IdeiaResponse.de(ideia);
	}

	public List<HistoricoIdeiaResponse> consultarHistorico(String id, String usuarioId) {
		Usuario usuario = usuarioAutenticadoService.buscarAtivo(usuarioId);
		Ideia ideia = buscarEntidade(id);
		validarConsulta(ideia, usuario);
		return ideia.getHistorico().stream()
				.sorted(Comparator.comparing(HistoricoIdeia::getDataHora).reversed())
				.map(HistoricoIdeiaResponse::de)
				.toList();
	}

	public PaginaResponse<IdeiaResponse> listarMinhas(int pagina, int tamanho, String usuarioId) {
		usuarioAutenticadoService.buscarAtivo(usuarioId);
		return executarConsulta(new Query(Criteria.where("autorId").is(usuarioId)), pagina, tamanho);
	}

	public PaginaResponse<IdeiaResponse> listar(StatusIdeia status, String categoria, String estrategiaId,
			Integer prioridade, int pagina, int tamanho, String usuarioId) {
		usuarioAutenticadoService.buscarAtivo(usuarioId);
		Query query = new Query();
		if (status != null) query.addCriteria(Criteria.where("status").is(status));
		if (StringUtils.hasText(categoria)) query.addCriteria(Criteria.where("categoria").regex(exata(categoria), "i"));
		if (StringUtils.hasText(estrategiaId)) query.addCriteria(Criteria.where("estrategiaId").is(estrategiaId.strip()));
		if (prioridade != null) query.addCriteria(Criteria.where("prioridade").is(prioridade));
		return executarConsulta(query, pagina, tamanho);
	}

	private PaginaResponse<IdeiaResponse> executarConsulta(Query query, int pagina, int tamanho) {
		long total = mongoTemplate.count(query, Ideia.class);
		PageRequest paginacao = PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.DESC, "criadoEm"));
		List<IdeiaResponse> conteudo = mongoTemplate.find(query.with(paginacao), Ideia.class).stream()
				.map(IdeiaResponse::de).toList();
		return PaginaResponse.de(conteudo, pagina, tamanho, total);
	}

	private void validarConsulta(Ideia ideia, Usuario usuario) {
		if (usuario.getRole() == Role.OPERADOR && !ideia.getAutorId().equals(usuario.getId())) {
			throw new IdeiaNaoEncontradaException();
		}
	}

	private void validarAutorEEnviada(Ideia ideia, String usuarioId) {
		if (!ideia.getAutorId().equals(usuarioId)) throw new IdeiaNaoEncontradaException();
		validarStatus(ideia, StatusIdeia.ENVIADA, "Somente uma ideia enviada pode ser alterada");
	}

	private void validarStatus(Ideia ideia, StatusIdeia esperado, String mensagem) {
		if (ideia.getStatus() != esperado) throw new OperacaoIdeiaInvalidaException(mensagem);
	}

	private void validarEstrategiaAtiva(String estrategiaId) {
		Estrategia estrategia = estrategiaRepository.findById(estrategiaId)
				.orElseThrow(EstrategiaNaoEncontradaException::new);
		if (estrategia.getStatus() != StatusEstrategia.ATIVA) {
			throw new OperacaoIdeiaInvalidaException("A estratégia deve estar ativa");
		}
	}

	private Ideia buscarEntidade(String id) {
		return ideiaRepository.findById(id).orElseThrow(IdeiaNaoEncontradaException::new);
	}

	private void aplicarDados(Ideia ideia, String titulo, String problema, String solucaoProposta,
			String beneficiosEsperados, String categoria, String estrategiaId) {
		ideia.setTitulo(titulo);
		ideia.setProblema(problema);
		ideia.setSolucaoProposta(solucaoProposta);
		ideia.setBeneficiosEsperados(beneficiosEsperados);
		ideia.setCategoria(categoria);
		ideia.setEstrategiaId(estrategiaId);
	}

	private void registrarHistorico(Ideia ideia, AcaoHistoricoIdeia acao, String usuarioId) {
		ideia.adicionarHistorico(new HistoricoIdeia(UUID.randomUUID().toString(), acao, Instant.now(), usuarioId,
				ideia.getTitulo(), ideia.getProblema(), ideia.getSolucaoProposta(), ideia.getBeneficiosEsperados(),
				ideia.getCategoria(), ideia.getEstrategiaId(), ideia.getStatus(), ideia.getPrioridade(),
				ideia.getJustificativaAvaliacao()));
	}

	private String exata(String valor) {
		return "^" + Pattern.quote(valor.strip()) + "$";
	}
}
