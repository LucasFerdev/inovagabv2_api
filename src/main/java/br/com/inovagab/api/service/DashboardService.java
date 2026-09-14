package br.com.inovagab.api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationExpression;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.BooleanOperators;
import org.springframework.data.mongodb.core.aggregation.ComparisonOperators;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import br.com.inovagab.api.dto.DashboardEstrategiaReferenciaResponse;
import br.com.inovagab.api.dto.DashboardEstrategiaResponse;
import br.com.inovagab.api.dto.DashboardIdeiaReferenciaResponse;
import br.com.inovagab.api.dto.DashboardProjetoResponse;
import br.com.inovagab.api.dto.DashboardResumoResponse;
import br.com.inovagab.api.exception.EstrategiaNaoEncontradaException;
import br.com.inovagab.api.exception.IdeiaNaoEncontradaException;
import br.com.inovagab.api.exception.ProjetoNaoEncontradoException;
import br.com.inovagab.api.model.Estrategia;
import br.com.inovagab.api.model.Ideia;
import br.com.inovagab.api.model.Projeto;
import br.com.inovagab.api.model.StatusIdeia;
import br.com.inovagab.api.model.StatusProjeto;
import br.com.inovagab.api.repository.EstrategiaRepository;
import br.com.inovagab.api.repository.IdeiaRepository;
import br.com.inovagab.api.repository.ProjetoRepository;

@Service
public class DashboardService {

	private static final int ESCALA = 2;
	private final MongoTemplate mongoTemplate;
	private final ProjetoRepository projetoRepository;
	private final EstrategiaRepository estrategiaRepository;
	private final IdeiaRepository ideiaRepository;
	private final UsuarioAutenticadoService usuarioAutenticadoService;

	public DashboardService(MongoTemplate mongoTemplate, ProjetoRepository projetoRepository,
			EstrategiaRepository estrategiaRepository, IdeiaRepository ideiaRepository,
			UsuarioAutenticadoService usuarioAutenticadoService) {
		this.mongoTemplate = mongoTemplate;
		this.projetoRepository = projetoRepository;
		this.estrategiaRepository = estrategiaRepository;
		this.ideiaRepository = ideiaRepository;
		this.usuarioAutenticadoService = usuarioAutenticadoService;
	}

	public DashboardResumoResponse resumo(String usuarioId) {
		usuarioAutenticadoService.buscarAtivo(usuarioId);
		Document projetos = primeiro(mongoTemplate.aggregate(agregacaoProjetos(null), Projeto.class, Document.class));
		Document ideias = primeiro(mongoTemplate.aggregate(agregacaoIdeias(null), Ideia.class, Document.class));
		BigDecimal investimento = decimal(projetos, "investimento");
		BigDecimal retorno = decimal(projetos, "retorno");
		Map<StatusProjeto, Long> projetosPorStatus = distribuicaoProjetos(projetos);
		Map<StatusIdeia, Long> ideiasPorStatus = distribuicaoIdeias(ideias);
		return new DashboardResumoResponse(numero(projetos, "total"), numero(projetos, "planejado"),
				numero(projetos, "emAndamento"), numero(projetos, "pausado"), numero(projetos, "concluido"),
				numero(projetos, "cancelado"), dinheiro(investimento), dinheiro(retorno), dinheiro(retorno.subtract(investimento)),
				roi(investimento, retorno), percentual(decimal(projetos, "progressoMedio")),
				percentual(decimal(projetos, "produtividadeMedia")), numero(projetos, "atrasados"),
				numero(ideias, "enviadas"), numero(ideias, "emAnalise"), numero(ideias, "aprovadas"),
				numero(ideias, "rejeitadas"), projetosPorStatus, ideiasPorStatus);
	}

	public DashboardEstrategiaResponse porEstrategia(String estrategiaId, String usuarioId) {
		usuarioAutenticadoService.buscarAtivo(usuarioId);
		Estrategia estrategia = estrategiaRepository.findById(estrategiaId)
				.orElseThrow(EstrategiaNaoEncontradaException::new);
		Document projetos = primeiro(mongoTemplate.aggregate(agregacaoProjetos(estrategiaId), Projeto.class, Document.class));
		Document ideias = primeiro(mongoTemplate.aggregate(agregacaoIdeias(estrategiaId), Ideia.class, Document.class));
		BigDecimal investimento = decimal(projetos, "investimento");
		BigDecimal retorno = decimal(projetos, "retorno");
		return new DashboardEstrategiaResponse(estrategia.getId(), estrategia.getTitulo(), estrategia.getStatus(),
				numero(ideias, "total"), numero(ideias, "aprovadas"), numero(projetos, "total"), dinheiro(investimento),
				dinheiro(retorno), dinheiro(retorno.subtract(investimento)), roi(investimento, retorno),
				percentual(decimal(projetos, "progressoMedio")), percentual(decimal(projetos, "produtividadeMedia")),
				numero(projetos, "atrasados"));
	}

	public DashboardProjetoResponse porProjeto(String projetoId, String usuarioId) {
		usuarioAutenticadoService.buscarAtivo(usuarioId);
		Projeto projeto = projetoRepository.findById(projetoId).orElseThrow(ProjetoNaoEncontradoException::new);
		Estrategia estrategia = estrategiaRepository.findById(projeto.getEstrategiaId())
				.orElseThrow(EstrategiaNaoEncontradaException::new);
		DashboardIdeiaReferenciaResponse ideiaResponse = null;
		if (projeto.getIdeiaOrigemId() != null) {
			Ideia ideia = ideiaRepository.findById(projeto.getIdeiaOrigemId()).orElseThrow(IdeiaNaoEncontradaException::new);
			ideiaResponse = new DashboardIdeiaReferenciaResponse(ideia.getId(), ideia.getTitulo(), ideia.getStatus());
		}
		BigDecimal investimento = valor(projeto.getInvestimento());
		BigDecimal retorno = valor(projeto.getRetornoFinanceiro());
		return new DashboardProjetoResponse(projeto.getId(), projeto.getNome(), projeto.getDescricao(),
				new DashboardEstrategiaReferenciaResponse(estrategia.getId(), estrategia.getTitulo(), estrategia.getStatus()),
				ideiaResponse, projeto.getEtapa(), projeto.getStatus(), projeto.getPercentualProgresso(),
				dinheiro(investimento), dinheiro(retorno), dinheiro(retorno.subtract(investimento)), roi(investimento, retorno),
				projeto.getPrazo(), atrasado(projeto), percentual(valor(projeto.getGanhoProdutividadePercentual())),
				projeto.getResultado());
	}

	private Aggregation agregacaoProjetos(String estrategiaId) {
		AggregationExpression atrasado = BooleanOperators.And.and(
				ComparisonOperators.Lt.valueOf("prazo").lessThanValue(dataAtual()),
				ComparisonOperators.Ne.valueOf("status").notEqualToValue(StatusProjeto.CONCLUIDO.name()),
				ComparisonOperators.Ne.valueOf("status").notEqualToValue(StatusProjeto.CANCELADO.name()));
		List<org.springframework.data.mongodb.core.aggregation.AggregationOperation> operacoes = new java.util.ArrayList<>();
		if (estrategiaId != null) operacoes.add(Aggregation.match(Criteria.where("estrategiaId").is(estrategiaId)));
		operacoes.add(Aggregation.group().count().as("total")
				.sum(condicao("status", StatusProjeto.PLANEJADO)).as("planejado")
				.sum(condicao("status", StatusProjeto.EM_ANDAMENTO)).as("emAndamento")
				.sum(condicao("status", StatusProjeto.PAUSADO)).as("pausado")
				.sum(condicao("status", StatusProjeto.CONCLUIDO)).as("concluido")
				.sum(condicao("status", StatusProjeto.CANCELADO)).as("cancelado")
				.sum("investimento").as("investimento").sum("retornoFinanceiro").as("retorno")
				.avg("percentualProgresso").as("progressoMedio")
				.avg("ganhoProdutividadePercentual").as("produtividadeMedia")
				.sum(ConditionalOperators.when(atrasado).then(1).otherwise(0)).as("atrasados"));
		return Aggregation.newAggregation(operacoes);
	}

	private Aggregation agregacaoIdeias(String estrategiaId) {
		List<org.springframework.data.mongodb.core.aggregation.AggregationOperation> operacoes = new java.util.ArrayList<>();
		if (estrategiaId != null) operacoes.add(Aggregation.match(Criteria.where("estrategiaId").is(estrategiaId)));
		operacoes.add(Aggregation.group().count().as("total")
				.sum(condicao("status", StatusIdeia.ENVIADA)).as("enviadas")
				.sum(condicao("status", StatusIdeia.EM_ANALISE)).as("emAnalise")
				.sum(condicao("status", StatusIdeia.APROVADA)).as("aprovadas")
				.sum(condicao("status", StatusIdeia.REJEITADA)).as("rejeitadas")
				.sum(condicao("status", StatusIdeia.ARQUIVADA)).as("arquivadas"));
		return Aggregation.newAggregation(operacoes);
	}

	private ConditionalOperators.Cond condicao(String campo, Enum<?> valor) {
		return ConditionalOperators.when(Criteria.where(campo).is(valor)).then(1).otherwise(0);
	}

	private Document primeiro(AggregationResults<Document> resultados) {
		return resultados.getUniqueMappedResult() == null ? new Document() : resultados.getUniqueMappedResult();
	}

	private Map<StatusProjeto, Long> distribuicaoProjetos(Document dados) {
		Map<StatusProjeto, Long> mapa = new EnumMap<>(StatusProjeto.class);
		mapa.put(StatusProjeto.PLANEJADO, numero(dados, "planejado"));
		mapa.put(StatusProjeto.EM_ANDAMENTO, numero(dados, "emAndamento"));
		mapa.put(StatusProjeto.PAUSADO, numero(dados, "pausado"));
		mapa.put(StatusProjeto.CONCLUIDO, numero(dados, "concluido"));
		mapa.put(StatusProjeto.CANCELADO, numero(dados, "cancelado"));
		return mapa;
	}

	private Map<StatusIdeia, Long> distribuicaoIdeias(Document dados) {
		Map<StatusIdeia, Long> mapa = new EnumMap<>(StatusIdeia.class);
		mapa.put(StatusIdeia.ENVIADA, numero(dados, "enviadas"));
		mapa.put(StatusIdeia.EM_ANALISE, numero(dados, "emAnalise"));
		mapa.put(StatusIdeia.APROVADA, numero(dados, "aprovadas"));
		mapa.put(StatusIdeia.REJEITADA, numero(dados, "rejeitadas"));
		mapa.put(StatusIdeia.ARQUIVADA, numero(dados, "arquivadas"));
		return mapa;
	}

	private long numero(Document documento, String campo) {
		Object valor = documento.get(campo);
		return valor instanceof Number numero ? numero.longValue() : 0L;
	}

	private BigDecimal decimal(Document documento, String campo) {
		return decimal(documento.get(campo));
	}

	private BigDecimal decimal(Object objeto) {
		if (objeto instanceof Decimal128 decimal) return decimal.bigDecimalValue();
		if (objeto instanceof BigDecimal decimal) return decimal;
		if (objeto instanceof Number numero) return BigDecimal.valueOf(numero.doubleValue());
		return BigDecimal.ZERO;
	}

	private BigDecimal valor(BigDecimal valor) { return valor == null ? BigDecimal.ZERO : valor; }
	private BigDecimal dinheiro(BigDecimal valor) { return valor.setScale(ESCALA, RoundingMode.HALF_UP); }
	private BigDecimal percentual(BigDecimal valor) { return valor.setScale(ESCALA, RoundingMode.HALF_UP); }
	private Date dataAtual() {
		return Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
	}

	private BigDecimal roi(BigDecimal investimento, BigDecimal retorno) {
		if (investimento.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO.setScale(ESCALA);
		return retorno.subtract(investimento).multiply(BigDecimal.valueOf(100))
				.divide(investimento, ESCALA, RoundingMode.HALF_UP);
	}

	private boolean atrasado(Projeto projeto) {
		return projeto.getPrazo() != null && projeto.getPrazo().isBefore(LocalDate.now())
				&& projeto.getStatus() != StatusProjeto.CONCLUIDO && projeto.getStatus() != StatusProjeto.CANCELADO;
	}
}
