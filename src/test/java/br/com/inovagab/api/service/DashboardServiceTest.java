package br.com.inovagab.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;

import br.com.inovagab.api.dto.DashboardEstrategiaResponse;
import br.com.inovagab.api.dto.DashboardProjetoResponse;
import br.com.inovagab.api.dto.DashboardResumoResponse;
import br.com.inovagab.api.exception.EstrategiaNaoEncontradaException;
import br.com.inovagab.api.exception.ProjetoNaoEncontradoException;
import br.com.inovagab.api.model.Estrategia;
import br.com.inovagab.api.model.EtapaProjeto;
import br.com.inovagab.api.model.Ideia;
import br.com.inovagab.api.model.Projeto;
import br.com.inovagab.api.model.StatusEstrategia;
import br.com.inovagab.api.model.StatusIdeia;
import br.com.inovagab.api.model.StatusProjeto;
import br.com.inovagab.api.model.Usuario;
import br.com.inovagab.api.repository.EstrategiaRepository;
import br.com.inovagab.api.repository.IdeiaRepository;
import br.com.inovagab.api.repository.ProjetoRepository;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

	@Mock private MongoTemplate mongoTemplate;
	@Mock private ProjetoRepository projetoRepository;
	@Mock private EstrategiaRepository estrategiaRepository;
	@Mock private IdeiaRepository ideiaRepository;
	@Mock private UsuarioAutenticadoService usuarioAutenticadoService;
	private DashboardService dashboardService;

	@BeforeEach
	void configurar() {
		dashboardService = new DashboardService(mongoTemplate, projetoRepository, estrategiaRepository, ideiaRepository,
				usuarioAutenticadoService);
		lenient().when(usuarioAutenticadoService.buscarAtivo("lider-id")).thenReturn(new Usuario());
	}

	@Test
	void resumoSemProjetosRetornaIndicadoresZerados() {
		agregacoes(new Document(), new Document());
		DashboardResumoResponse response = dashboardService.resumo("lider-id");
		assertThat(response.totalProjetos()).isZero();
		assertThat(response.investimentoTotal()).isEqualByComparingTo("0.00");
		assertThat(response.roiPercentual()).isEqualByComparingTo("0.00");
		assertThat(response.projetosPorStatus()).allSatisfy((status, total) -> assertThat(total).isZero());
	}

	@Test
	void resumoCalculaLucroRoiMediasAtrasosEDistribuicoes() {
		Document projetos = new Document("total", 5).append("planejado", 1).append("emAndamento", 1)
				.append("pausado", 1).append("concluido", 1).append("cancelado", 1)
				.append("investimento", new BigDecimal("1000.00")).append("retorno", new BigDecimal("1250.00"))
				.append("progressoMedio", 47.555).append("produtividadeMedia", 18.125).append("atrasados", 2);
		Document ideias = new Document("total", 7).append("enviadas", 2).append("emAnalise", 1)
				.append("aprovadas", 2).append("rejeitadas", 1).append("arquivadas", 1);
		agregacoes(projetos, ideias);
		DashboardResumoResponse response = dashboardService.resumo("lider-id");
		assertThat(response.lucroObtido()).isEqualByComparingTo("250.00");
		assertThat(response.roiPercentual()).isEqualByComparingTo("25.00");
		assertThat(response.progressoMedio()).isEqualByComparingTo("47.56");
		assertThat(response.ganhoMedioProdutividade()).isEqualByComparingTo("18.13");
		assertThat(response.projetosAtrasados()).isEqualTo(2);
		assertThat(response.ideiasAprovadas()).isEqualTo(2);
	}

	@Test
	void investimentoZeroProduzRoiZero() {
		agregacoes(new Document("investimento", BigDecimal.ZERO).append("retorno", new BigDecimal("100.00")),
				new Document());
		assertThat(dashboardService.resumo("lider-id").roiPercentual()).isEqualByComparingTo("0.00");
	}

	@Test
	void roiUtilizaArredondamentoExplicito() {
		agregacoes(new Document("investimento", new BigDecimal("3.00")).append("retorno", new BigDecimal("4.00")),
				new Document());
		assertThat(dashboardService.resumo("lider-id").roiPercentual()).isEqualByComparingTo("33.33");
	}

	@Test
	void dashboardPorEstrategiaAgregaSomenteRelacionados() {
		Estrategia estrategia = estrategia();
		when(estrategiaRepository.findById("estrategia-id")).thenReturn(Optional.of(estrategia));
		agregacoes(new Document("total", 2).append("investimento", new BigDecimal("500.00"))
				.append("retorno", new BigDecimal("750.00")).append("progressoMedio", 50).append("produtividadeMedia", 10)
				.append("atrasados", 1), new Document("total", 3).append("aprovadas", 2));
		DashboardEstrategiaResponse response = dashboardService.porEstrategia("estrategia-id", "lider-id");
		assertThat(response.quantidadeProjetos()).isEqualTo(2);
		assertThat(response.quantidadeIdeias()).isEqualTo(3);
		assertThat(response.roiPercentual()).isEqualByComparingTo("50.00");
	}

	@Test
	void dashboardPorProjetoCalculaIndicadoresEAtraso() {
		Projeto projeto = projeto(StatusProjeto.EM_ANDAMENTO, LocalDate.now().minusDays(1));
		when(projetoRepository.findById("projeto-id")).thenReturn(Optional.of(projeto));
		when(estrategiaRepository.findById("estrategia-id")).thenReturn(Optional.of(estrategia()));
		Ideia ideia = new Ideia(); ideia.setId("ideia-id"); ideia.setTitulo("Ideia"); ideia.setStatus(StatusIdeia.APROVADA);
		when(ideiaRepository.findById("ideia-id")).thenReturn(Optional.of(ideia));
		DashboardProjetoResponse response = dashboardService.porProjeto("projeto-id", "lider-id");
		assertThat(response.lucro()).isEqualByComparingTo("250.00");
		assertThat(response.roiPercentual()).isEqualByComparingTo("25.00");
		assertThat(response.atrasado()).isTrue();
		assertThat(response.ideiaOrigem().id()).isEqualTo("ideia-id");
	}

	@Test
	void projetoConcluidoNaoEConsideradoAtrasado() {
		Projeto projeto = projeto(StatusProjeto.CONCLUIDO, LocalDate.now().minusDays(10));
		projeto.setIdeiaOrigemId(null);
		when(projetoRepository.findById("projeto-id")).thenReturn(Optional.of(projeto));
		when(estrategiaRepository.findById("estrategia-id")).thenReturn(Optional.of(estrategia()));
		assertThat(dashboardService.porProjeto("projeto-id", "lider-id").atrasado()).isFalse();
	}

	@Test
	void projetoEEstrategiaInexistentesRetornamErrosEspecificos() {
		when(projetoRepository.findById("inexistente")).thenReturn(Optional.empty());
		assertThatThrownBy(() -> dashboardService.porProjeto("inexistente", "lider-id"))
				.isInstanceOf(ProjetoNaoEncontradoException.class);
		when(estrategiaRepository.findById("inexistente")).thenReturn(Optional.empty());
		assertThatThrownBy(() -> dashboardService.porEstrategia("inexistente", "lider-id"))
				.isInstanceOf(EstrategiaNaoEncontradaException.class);
	}

	private void agregacoes(Document projetos, Document ideias) {
		when(mongoTemplate.aggregate(any(Aggregation.class), eq(Projeto.class), eq(Document.class)))
				.thenReturn(resultados(projetos));
		when(mongoTemplate.aggregate(any(Aggregation.class), eq(Ideia.class), eq(Document.class)))
				.thenReturn(resultados(ideias));
	}

	private AggregationResults<Document> resultados(Document documento) {
		return documento.isEmpty() ? new AggregationResults<>(List.of(), new Document())
				: new AggregationResults<>(List.of(documento), new Document());
	}

	private Estrategia estrategia() {
		Estrategia estrategia = new Estrategia(); estrategia.setId("estrategia-id");
		estrategia.setTitulo("Estratégia ativa"); estrategia.setStatus(StatusEstrategia.ATIVA); return estrategia;
	}

	private Projeto projeto(StatusProjeto status, LocalDate prazo) {
		Projeto projeto = new Projeto(); projeto.setId("projeto-id"); projeto.setNome("Projeto");
		projeto.setDescricao("Descrição do projeto"); projeto.setEstrategiaId("estrategia-id");
		projeto.setIdeiaOrigemId("ideia-id"); projeto.setEtapa(EtapaProjeto.DESENVOLVIMENTO); projeto.setStatus(status);
		projeto.setPercentualProgresso(50); projeto.setInvestimento(new BigDecimal("1000.00"));
		projeto.setRetornoFinanceiro(new BigDecimal("1250.00")); projeto.setGanhoProdutividadePercentual(new BigDecimal("18.5"));
		projeto.setPrazo(prazo); projeto.setResultado("Resultado seguro"); return projeto;
	}
}
