package br.com.inovagab.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
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

import br.com.inovagab.api.dto.AtualizarIdeiaRequest;
import br.com.inovagab.api.dto.CriarIdeiaRequest;
import br.com.inovagab.api.dto.IdeiaResponse;
import br.com.inovagab.api.dto.PaginaResponse;
import br.com.inovagab.api.dto.PriorizarIdeiaRequest;
import br.com.inovagab.api.dto.RejeitarIdeiaRequest;
import br.com.inovagab.api.exception.EstrategiaNaoEncontradaException;
import br.com.inovagab.api.exception.IdeiaNaoEncontradaException;
import br.com.inovagab.api.exception.OperacaoIdeiaInvalidaException;
import br.com.inovagab.api.model.AcaoHistoricoIdeia;
import br.com.inovagab.api.model.Estrategia;
import br.com.inovagab.api.model.Ideia;
import br.com.inovagab.api.model.Role;
import br.com.inovagab.api.model.StatusEstrategia;
import br.com.inovagab.api.model.StatusIdeia;
import br.com.inovagab.api.model.Usuario;
import br.com.inovagab.api.repository.EstrategiaRepository;
import br.com.inovagab.api.repository.IdeiaRepository;

@ExtendWith(MockitoExtension.class)
class IdeiaServiceTest {

	private static final String USUARIO_ID = "operador-id";

	@Mock private IdeiaRepository ideiaRepository;
	@Mock private EstrategiaRepository estrategiaRepository;
	@Mock private MongoTemplate mongoTemplate;
	@Mock private UsuarioAutenticadoService usuarioAutenticadoService;
	private IdeiaService ideiaService;

	@BeforeEach
	void configurar() {
		ideiaService = new IdeiaService(ideiaRepository, estrategiaRepository, mongoTemplate,
				usuarioAutenticadoService);
		lenient().when(usuarioAutenticadoService.buscarAtivo(USUARIO_ID))
				.thenReturn(usuario(USUARIO_ID, Role.OPERADOR));
	}

	@Test
	void operadorCriaIdeiaNormalizadaVinculadaAEstrategiaAtivaComHistorico() {
		when(estrategiaRepository.findById("estrategia-id")).thenReturn(Optional.of(estrategia(StatusEstrategia.ATIVA)));
		when(ideiaRepository.save(any(Ideia.class))).thenAnswer(invocation -> invocation.getArgument(0));

		IdeiaResponse response = ideiaService.criar(requestCriacao(), USUARIO_ID);

		ArgumentCaptor<Ideia> captor = ArgumentCaptor.forClass(Ideia.class);
		verify(ideiaRepository).save(captor.capture());
		Ideia ideia = captor.getValue();
		assertThat(ideia.getTitulo()).isEqualTo("Ideia inovadora");
		assertThat(ideia.getAutorId()).isEqualTo(USUARIO_ID);
		assertThat(ideia.getStatus()).isEqualTo(StatusIdeia.ENVIADA);
		assertThat(ideia.getPrioridade()).isNull();
		assertThat(ideia.getHistorico()).singleElement().satisfies(item -> {
			assertThat(item.getAcao()).isEqualTo(AcaoHistoricoIdeia.CRIADA);
			assertThat(item.getTitulo()).isEqualTo("Ideia inovadora");
		});
		assertThat(response.status()).isEqualTo(StatusIdeia.ENVIADA);
	}

	@Test
	void estrategiaInexistenteInativaOuArquivadaImpedeCriacao() {
		when(estrategiaRepository.findById("estrategia-id")).thenReturn(Optional.empty());
		assertThatThrownBy(() -> ideiaService.criar(requestCriacao(), USUARIO_ID))
				.isInstanceOf(EstrategiaNaoEncontradaException.class);

		when(estrategiaRepository.findById("estrategia-id")).thenReturn(Optional.of(estrategia(StatusEstrategia.INATIVA)));
		assertThatThrownBy(() -> ideiaService.criar(requestCriacao(), USUARIO_ID))
				.isInstanceOf(OperacaoIdeiaInvalidaException.class);

		when(estrategiaRepository.findById("estrategia-id")).thenReturn(Optional.of(estrategia(StatusEstrategia.ARQUIVADA)));
		assertThatThrownBy(() -> ideiaService.criar(requestCriacao(), USUARIO_ID))
				.isInstanceOf(OperacaoIdeiaInvalidaException.class);
		verify(ideiaRepository, never()).save(any());
	}

	@Test
	void operadorConsultaSomentePropriasIdeias() {
		Ideia propria = ideia(StatusIdeia.ENVIADA, USUARIO_ID);
		when(ideiaRepository.findById("ideia-id")).thenReturn(Optional.of(propria));
		assertThat(ideiaService.buscarPorId("ideia-id", USUARIO_ID).autorId()).isEqualTo(USUARIO_ID);

		propria.setAutorId("outro-id");
		assertThatThrownBy(() -> ideiaService.buscarPorId("ideia-id", USUARIO_ID))
				.isInstanceOf(IdeiaNaoEncontradaException.class);
	}

	@Test
	void operadorAtualizaEArquivaSomenteIdeiaEnviada() {
		Ideia ideia = ideia(StatusIdeia.ENVIADA, USUARIO_ID);
		when(ideiaRepository.findById("ideia-id")).thenReturn(Optional.of(ideia));
		when(estrategiaRepository.findById("estrategia-id")).thenReturn(Optional.of(estrategia(StatusEstrategia.ATIVA)));
		when(ideiaRepository.save(ideia)).thenReturn(ideia);

		ideiaService.atualizar("ideia-id", requestAtualizacao(), USUARIO_ID);
		assertThat(ideia.getHistorico()).last().extracting("acao").isEqualTo(AcaoHistoricoIdeia.ATUALIZADA);
		ideiaService.arquivar("ideia-id", USUARIO_ID);
		assertThat(ideia.getStatus()).isEqualTo(StatusIdeia.ARQUIVADA);
		assertThat(ideia.getHistorico()).last().extracting("acao").isEqualTo(AcaoHistoricoIdeia.ARQUIVADA);
	}

	@Test
	void operadorNaoAlteraIdeiaEmAnalise() {
		when(ideiaRepository.findById("ideia-id")).thenReturn(Optional.of(ideia(StatusIdeia.EM_ANALISE, USUARIO_ID)));
		assertThatThrownBy(() -> ideiaService.atualizar("ideia-id", requestAtualizacao(), USUARIO_ID))
				.isInstanceOf(OperacaoIdeiaInvalidaException.class);
	}

	@Test
	void gestorAnalisaPriorizaEAprovaComHistorico() {
		Ideia ideia = ideia(StatusIdeia.ENVIADA, USUARIO_ID);
		when(usuarioAutenticadoService.buscarAtivo("gestor-id")).thenReturn(usuario("gestor-id", Role.GESTOR));
		when(ideiaRepository.findById("ideia-id")).thenReturn(Optional.of(ideia));
		when(ideiaRepository.save(ideia)).thenReturn(ideia);

		ideiaService.analisar("ideia-id", "gestor-id");
		ideiaService.priorizar("ideia-id", new PriorizarIdeiaRequest(5, " Alto impacto "), "gestor-id");
		IdeiaResponse response = ideiaService.aprovar("ideia-id", "gestor-id");

		assertThat(response.status()).isEqualTo(StatusIdeia.APROVADA);
		assertThat(ideia.getPrioridade()).isEqualTo(5);
		assertThat(ideia.getJustificativaAvaliacao()).isEqualTo("Alto impacto");
		assertThat(ideia.getHistorico()).extracting("acao").containsExactly(
				AcaoHistoricoIdeia.ENVIADA_PARA_ANALISE, AcaoHistoricoIdeia.PRIORIZADA,
				AcaoHistoricoIdeia.APROVADA);
	}

	@Test
	void aprovacaoSemPrioridadeRetornaConflito() {
		when(usuarioAutenticadoService.buscarAtivo("gestor-id")).thenReturn(usuario("gestor-id", Role.GESTOR));
		when(ideiaRepository.findById("ideia-id")).thenReturn(Optional.of(ideia(StatusIdeia.EM_ANALISE, USUARIO_ID)));
		assertThatThrownBy(() -> ideiaService.aprovar("ideia-id", "gestor-id"))
				.isInstanceOf(OperacaoIdeiaInvalidaException.class);
	}

	@Test
	void gestorRejeitaComJustificativa() {
		Ideia ideia = ideia(StatusIdeia.EM_ANALISE, USUARIO_ID);
		when(usuarioAutenticadoService.buscarAtivo("gestor-id")).thenReturn(usuario("gestor-id", Role.GESTOR));
		when(ideiaRepository.findById("ideia-id")).thenReturn(Optional.of(ideia));
		when(ideiaRepository.save(ideia)).thenReturn(ideia);
		IdeiaResponse response = ideiaService.rejeitar("ideia-id", new RejeitarIdeiaRequest(" Baixa aderência "), "gestor-id");
		assertThat(response.status()).isEqualTo(StatusIdeia.REJEITADA);
		assertThat(response.justificativaAvaliacao()).isEqualTo("Baixa aderência");
	}

	@Test
	void listagensAplicamAutoriaPaginacaoEFiltros() {
		when(mongoTemplate.count(any(Query.class), eq(Ideia.class))).thenReturn(1L);
		when(mongoTemplate.find(any(Query.class), eq(Ideia.class))).thenReturn(List.of(ideia(StatusIdeia.ENVIADA, USUARIO_ID)));

		PaginaResponse<IdeiaResponse> minhas = ideiaService.listarMinhas(0, 20, USUARIO_ID);
		PaginaResponse<IdeiaResponse> todas = ideiaService.listar(StatusIdeia.ENVIADA, " Inovação ",
				" estrategia-id ", 5, 0, 20, USUARIO_ID);

		ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
		verify(mongoTemplate, org.mockito.Mockito.times(2)).find(captor.capture(), eq(Ideia.class));
		Document minhasQuery = captor.getAllValues().get(0).getQueryObject();
		Document filtros = captor.getAllValues().get(1).getQueryObject();
		assertThat(minhasQuery).containsKey("autorId");
		assertThat(filtros).containsKeys("status", "categoria", "estrategiaId", "prioridade");
		assertThat(todas.totalElementos()).isEqualTo(1);
		assertThat(minhas.conteudo()).hasSize(1);
	}

	@Test
	void historicoRetornaMaisRecentePrimeiro() {
		Ideia ideia = ideia(StatusIdeia.EM_ANALISE, USUARIO_ID);
		ideia.adicionarHistorico(new br.com.inovagab.api.model.HistoricoIdeia("1", AcaoHistoricoIdeia.CRIADA,
				Instant.parse("2027-01-01T00:00:00Z"), USUARIO_ID, ideia.getTitulo(), ideia.getProblema(),
				ideia.getSolucaoProposta(), ideia.getBeneficiosEsperados(), ideia.getCategoria(), ideia.getEstrategiaId(),
				StatusIdeia.ENVIADA, null, null));
		ideia.adicionarHistorico(new br.com.inovagab.api.model.HistoricoIdeia("2", AcaoHistoricoIdeia.ENVIADA_PARA_ANALISE,
				Instant.parse("2027-01-02T00:00:00Z"), "gestor-id", ideia.getTitulo(), ideia.getProblema(),
				ideia.getSolucaoProposta(), ideia.getBeneficiosEsperados(), ideia.getCategoria(), ideia.getEstrategiaId(),
				StatusIdeia.EM_ANALISE, null, null));
		when(ideiaRepository.findById("ideia-id")).thenReturn(Optional.of(ideia));
		assertThat(ideiaService.consultarHistorico("ideia-id", USUARIO_ID)).extracting("acao")
				.containsExactly(AcaoHistoricoIdeia.ENVIADA_PARA_ANALISE, AcaoHistoricoIdeia.CRIADA);
	}

	private CriarIdeiaRequest requestCriacao() {
		return new CriarIdeiaRequest(" Ideia inovadora ", " Problema operacional relevante ",
				" Solução proposta detalhada ", " Benefícios esperados relevantes ", " Inovação ", " estrategia-id ");
	}

	private AtualizarIdeiaRequest requestAtualizacao() {
		return new AtualizarIdeiaRequest(" Ideia atualizada ", " Problema atualizado relevante ",
				" Solução atualizada detalhada ", " Benefícios atualizados relevantes ", " Inovação ", " estrategia-id ");
	}

	private Estrategia estrategia(StatusEstrategia status) {
		Estrategia estrategia = new Estrategia();
		estrategia.setId("estrategia-id");
		estrategia.setStatus(status);
		return estrategia;
	}

	private Ideia ideia(StatusIdeia status, String autorId) {
		Ideia ideia = new Ideia();
		ideia.setId("ideia-id");
		ideia.setTitulo("Ideia inovadora");
		ideia.setProblema("Problema operacional relevante");
		ideia.setSolucaoProposta("Solução proposta detalhada");
		ideia.setBeneficiosEsperados("Benefícios esperados relevantes");
		ideia.setCategoria("Inovação");
		ideia.setEstrategiaId("estrategia-id");
		ideia.setAutorId(autorId);
		ideia.setStatus(status);
		return ideia;
	}

	private Usuario usuario(String id, Role role) {
		Usuario usuario = new Usuario();
		usuario.setId(id);
		usuario.setRole(role);
		usuario.setAtivo(true);
		return usuario;
	}
}
