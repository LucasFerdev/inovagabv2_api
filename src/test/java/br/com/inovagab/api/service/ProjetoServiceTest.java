package br.com.inovagab.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import br.com.inovagab.api.dto.AtualizarProgressoProjetoRequest;
import br.com.inovagab.api.dto.AtualizarProjetoRequest;
import br.com.inovagab.api.dto.CriarProjetoRequest;
import br.com.inovagab.api.dto.ProjetoResponse;
import br.com.inovagab.api.dto.RegistrarResultadosProjetoRequest;
import br.com.inovagab.api.exception.IdeiaNaoEncontradaException;
import br.com.inovagab.api.exception.OperacaoProjetoInvalidaException;
import br.com.inovagab.api.exception.ProjetoNaoEncontradoException;
import br.com.inovagab.api.model.AcaoHistoricoProjeto;
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
class ProjetoServiceTest {

	private static final String GESTOR_ID = "gestor-id";
	@Mock private ProjetoRepository projetoRepository;
	@Mock private EstrategiaRepository estrategiaRepository;
	@Mock private IdeiaRepository ideiaRepository;
	@Mock private MongoTemplate mongoTemplate;
	@Mock private UsuarioAutenticadoService usuarioAutenticadoService;
	private ProjetoService projetoService;

	@BeforeEach
	void configurar() {
		projetoService = new ProjetoService(projetoRepository, estrategiaRepository, ideiaRepository, mongoTemplate,
				usuarioAutenticadoService);
		lenient().when(usuarioAutenticadoService.buscarAtivo(GESTOR_ID)).thenReturn(new Usuario());
	}

	@Test
	void gestorCriaProjetoComValoresIniciaisEHistorico() {
		estrategiaAtiva();
		when(projetoRepository.save(any(Projeto.class))).thenAnswer(invocation -> invocation.getArgument(0));
		ProjetoResponse response = projetoService.criar(criarRequest(null), GESTOR_ID);
		assertThat(response.status()).isEqualTo(StatusProjeto.PLANEJADO);
		assertThat(response.etapa()).isEqualTo(EtapaProjeto.PLANEJAMENTO);
		assertThat(response.percentualProgresso()).isZero();
		assertThat(response.retornoFinanceiro()).isEqualByComparingTo(BigDecimal.ZERO);
		ArgumentCaptor<Projeto> captor = ArgumentCaptor.forClass(Projeto.class);
		verify(projetoRepository).save(captor.capture());
		assertThat(captor.getValue().getNome()).isEqualTo("Projeto inovador");
		assertThat(captor.getValue().getGestorResponsavelId()).isEqualTo(GESTOR_ID);
		assertThat(captor.getValue().getHistorico()).singleElement().extracting("acao")
				.isEqualTo(AcaoHistoricoProjeto.CRIADO);
	}

	@Test
	void estrategiaDeveExistirEEstarAtiva() {
		when(estrategiaRepository.findById("estrategia-id")).thenReturn(Optional.empty());
		assertThatThrownBy(() -> projetoService.criar(criarRequest(null), GESTOR_ID)).isInstanceOf(RuntimeException.class);
		when(estrategiaRepository.findById("estrategia-id")).thenReturn(Optional.of(estrategia(StatusEstrategia.INATIVA)));
		assertThatThrownBy(() -> projetoService.criar(criarRequest(null), GESTOR_ID))
				.isInstanceOf(OperacaoProjetoInvalidaException.class);
	}

	@Test
	void ideiaOpcionalDeveExistirEstarAprovadaETerMesmaEstrategia() {
		estrategiaAtiva();
		when(ideiaRepository.findById("ideia-id")).thenReturn(Optional.empty());
		assertThatThrownBy(() -> projetoService.criar(criarRequest("ideia-id"), GESTOR_ID))
				.isInstanceOf(IdeiaNaoEncontradaException.class);
		Ideia ideia = ideia(StatusIdeia.EM_ANALISE, "estrategia-id");
		when(ideiaRepository.findById("ideia-id")).thenReturn(Optional.of(ideia));
		assertThatThrownBy(() -> projetoService.criar(criarRequest("ideia-id"), GESTOR_ID))
				.isInstanceOf(OperacaoProjetoInvalidaException.class);
		ideia.setStatus(StatusIdeia.APROVADA);
		ideia.setEstrategiaId("outra-estrategia");
		assertThatThrownBy(() -> projetoService.criar(criarRequest("ideia-id"), GESTOR_ID))
				.isInstanceOf(OperacaoProjetoInvalidaException.class);
	}

	@Test
	void ideiaAprovadaPermiteCriacao() {
		estrategiaAtiva();
		when(ideiaRepository.findById("ideia-id")).thenReturn(Optional.of(ideia(StatusIdeia.APROVADA, "estrategia-id")));
		when(projetoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		assertThat(projetoService.criar(criarRequest("ideia-id"), GESTOR_ID).ideiaOrigemId()).isEqualTo("ideia-id");
	}

	@Test
	void atualizacaoPreservaEstadoERegistraHistorico() {
		Projeto projeto = projeto(StatusProjeto.EM_ANDAMENTO);
		when(projetoRepository.findById("projeto-id")).thenReturn(Optional.of(projeto));
		estrategiaAtiva();
		when(projetoRepository.save(projeto)).thenReturn(projeto);
		projetoService.atualizar("projeto-id", new AtualizarProjetoRequest(" Novo nome ", " Nova descrição válida ",
				" estrategia-id ", null, new BigDecimal("2000.00"), LocalDate.of(2027, 12, 1)), GESTOR_ID);
		assertThat(projeto.getNome()).isEqualTo("Novo nome");
		assertThat(projeto.getStatus()).isEqualTo(StatusProjeto.EM_ANDAMENTO);
		assertThat(projeto.getHistorico()).last().extracting("acao").isEqualTo(AcaoHistoricoProjeto.ATUALIZADO);
	}

	@Test
	void progressoPodeAvancarEMasReducaoExigeJustificativa() {
		Projeto projeto = projeto(StatusProjeto.EM_ANDAMENTO);
		projeto.setPercentualProgresso(40);
		when(projetoRepository.findById("projeto-id")).thenReturn(Optional.of(projeto));
		when(projetoRepository.save(projeto)).thenReturn(projeto);
		assertThatThrownBy(() -> projetoService.atualizarProgresso("projeto-id",
				new AtualizarProgressoProjetoRequest(EtapaProjeto.DESENVOLVIMENTO, StatusProjeto.EM_ANDAMENTO, 30, null),
				GESTOR_ID)).isInstanceOf(OperacaoProjetoInvalidaException.class);
		ProjetoResponse response = projetoService.atualizarProgresso("projeto-id",
				new AtualizarProgressoProjetoRequest(EtapaProjeto.DESENVOLVIMENTO, StatusProjeto.EM_ANDAMENTO, 30,
						" Replanejamento "), GESTOR_ID);
		assertThat(response.percentualProgresso()).isEqualTo(30);
		assertThat(projeto.getHistorico()).last().extracting("justificativa").isEqualTo("Replanejamento");
	}

	@Test
	void resultadosSaoRegistradosComBigDecimal() {
		Projeto projeto = projeto(StatusProjeto.EM_ANDAMENTO);
		when(projetoRepository.findById("projeto-id")).thenReturn(Optional.of(projeto));
		when(projetoRepository.save(projeto)).thenReturn(projeto);
		ProjetoResponse response = projetoService.registrarResultados("projeto-id",
				new RegistrarResultadosProjetoRequest(new BigDecimal("250000.00"), new BigDecimal("18.5"),
						" Redução de custos "), GESTOR_ID);
		assertThat(response.retornoFinanceiro()).isEqualByComparingTo("250000.00");
		assertThat(response.resultado()).isEqualTo("Redução de custos");
	}

	@Test
	void conclusaoECancelamentoSaoLogicos() {
		Projeto projeto = projeto(StatusProjeto.EM_ANDAMENTO);
		when(projetoRepository.findById("projeto-id")).thenReturn(Optional.of(projeto));
		when(projetoRepository.save(projeto)).thenReturn(projeto);
		ProjetoResponse concluido = projetoService.concluir("projeto-id", GESTOR_ID);
		assertThat(concluido.status()).isEqualTo(StatusProjeto.CONCLUIDO);
		assertThat(concluido.etapa()).isEqualTo(EtapaProjeto.ENCERRAMENTO);
		assertThat(concluido.percentualProgresso()).isEqualTo(100);
		projeto.setStatus(StatusProjeto.EM_ANDAMENTO);
		projetoService.cancelar("projeto-id", GESTOR_ID);
		assertThat(projeto.getStatus()).isEqualTo(StatusProjeto.CANCELADO);
		verify(projetoRepository, never()).delete(any());
	}

	@Test
	void projetoFinalizadoNaoPodeSerAlterado() {
		when(projetoRepository.findById("projeto-id")).thenReturn(Optional.of(projeto(StatusProjeto.CONCLUIDO)));
		assertThatThrownBy(() -> projetoService.concluir("projeto-id", GESTOR_ID))
				.isInstanceOf(OperacaoProjetoInvalidaException.class);
	}

	@Test
	void listagemAplicaPaginacaoEFiltros() {
		when(mongoTemplate.count(any(Query.class), eq(Projeto.class))).thenReturn(1L);
		when(mongoTemplate.find(any(Query.class), eq(Projeto.class))).thenReturn(List.of(projeto(StatusProjeto.PLANEJADO)));
		projetoService.listar(StatusProjeto.PLANEJADO, EtapaProjeto.PLANEJAMENTO, " estrategia-id ", " gestor-id ",
				LocalDate.of(2027, 10, 1), 0, 20, GESTOR_ID);
		ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
		verify(mongoTemplate).find(captor.capture(), eq(Projeto.class));
		Document query = captor.getValue().getQueryObject();
		assertThat(query).containsKeys("status", "etapa", "estrategiaId", "gestorResponsavelId", "prazo");
	}

	@Test
	void projetoInexistenteRetorna404PeloHandler() {
		when(projetoRepository.findById("inexistente")).thenReturn(Optional.empty());
		assertThatThrownBy(() -> projetoService.buscarPorId("inexistente", GESTOR_ID))
				.isInstanceOf(ProjetoNaoEncontradoException.class);
	}

	private void estrategiaAtiva() {
		when(estrategiaRepository.findById("estrategia-id")).thenReturn(Optional.of(estrategia(StatusEstrategia.ATIVA)));
	}

	private CriarProjetoRequest criarRequest(String ideiaId) {
		return new CriarProjetoRequest(" Projeto inovador ", " Descrição suficientemente válida ", " estrategia-id ",
				ideiaId, new BigDecimal("1000.00"), LocalDate.of(2027, 10, 1));
	}

	private Estrategia estrategia(StatusEstrategia status) {
		Estrategia estrategia = new Estrategia(); estrategia.setId("estrategia-id"); estrategia.setStatus(status); return estrategia;
	}

	private Ideia ideia(StatusIdeia status, String estrategiaId) {
		Ideia ideia = new Ideia(); ideia.setId("ideia-id"); ideia.setStatus(status); ideia.setEstrategiaId(estrategiaId); return ideia;
	}

	private Projeto projeto(StatusProjeto status) {
		Projeto projeto = new Projeto(); projeto.setId("projeto-id"); projeto.setNome("Projeto inovador");
		projeto.setDescricao("Descrição suficientemente válida"); projeto.setEstrategiaId("estrategia-id");
		projeto.setEtapa(EtapaProjeto.PLANEJAMENTO); projeto.setStatus(status); projeto.setPercentualProgresso(0);
		projeto.setInvestimento(new BigDecimal("1000.00")); projeto.setPrazo(LocalDate.of(2027, 10, 1));
		projeto.setRetornoFinanceiro(BigDecimal.ZERO); projeto.setGanhoProdutividadePercentual(BigDecimal.ZERO);
		projeto.setGestorResponsavelId(GESTOR_ID); return projeto;
	}
}
