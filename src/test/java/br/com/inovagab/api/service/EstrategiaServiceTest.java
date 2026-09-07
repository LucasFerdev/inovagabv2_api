package br.com.inovagab.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
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
import br.com.inovagab.api.model.Usuario;
import br.com.inovagab.api.repository.EstrategiaRepository;

@ExtendWith(MockitoExtension.class)
class EstrategiaServiceTest {

	private static final String USUARIO_ID = "usuario-id";

	@Mock
	private EstrategiaRepository estrategiaRepository;

	@Mock
	private MongoTemplate mongoTemplate;

	@Mock
	private UsuarioAutenticadoService usuarioAutenticadoService;

	private EstrategiaService estrategiaService;

	@BeforeEach
	void configurar() {
		estrategiaService = new EstrategiaService(estrategiaRepository, mongoTemplate, usuarioAutenticadoService);
		when(usuarioAutenticadoService.buscarAtivo(USUARIO_ID)).thenReturn(new Usuario());
	}

	@Test
	void criacaoNormalizaDadosUsaRascunhoERegistraHistorico() {
		when(estrategiaRepository.save(any(Estrategia.class))).thenAnswer(invocation -> invocation.getArgument(0));

		EstrategiaResponse response = estrategiaService.criar(new CriarEstrategiaRequest(
				" Estratégia 2027 ",
				" Descrição completa da estratégia ",
				LocalDate.of(2027, 1, 10),
				" Inovação ",
				" Campanha anual "), USUARIO_ID);

		ArgumentCaptor<Estrategia> captor = ArgumentCaptor.forClass(Estrategia.class);
		verify(estrategiaRepository).save(captor.capture());
		Estrategia salva = captor.getValue();
		assertThat(salva.getTitulo()).isEqualTo("Estratégia 2027");
		assertThat(salva.getCategoria()).isEqualTo("Inovação");
		assertThat(salva.getStatus()).isEqualTo(StatusEstrategia.RASCUNHO);
		assertThat(salva.getCriadoPorId()).isEqualTo(USUARIO_ID);
		assertThat(salva.getHistorico()).singleElement().satisfies(historico -> {
			assertThat(historico.getAcao()).isEqualTo(AcaoHistoricoEstrategia.CRIADA);
			assertThat(historico.getStatus()).isEqualTo(StatusEstrategia.RASCUNHO);
			assertThat(historico.getUsuarioId()).isEqualTo(USUARIO_ID);
			assertThat(historico.getId()).isNotBlank();
			assertThat(historico.getTitulo()).isEqualTo("Estratégia 2027");
			assertThat(historico.getCategoria()).isEqualTo("Inovação");
		});
		assertThat(response.status()).isEqualTo(StatusEstrategia.RASCUNHO);
	}

	@Test
	void atualizacaoAlteraSomenteDadosEditaveisERegistraHistorico() {
		Estrategia estrategia = estrategia(StatusEstrategia.ATIVA);
		when(estrategiaRepository.findById("estrategia-id")).thenReturn(Optional.of(estrategia));
		when(estrategiaRepository.save(estrategia)).thenReturn(estrategia);

		estrategiaService.atualizar("estrategia-id", new AtualizarEstrategiaRequest(
				" Novo título ", " Nova descrição completa ", LocalDate.of(2027, 2, 1),
				" Nova categoria ", " Nova campanha "), USUARIO_ID);

		assertThat(estrategia.getTitulo()).isEqualTo("Novo título");
		assertThat(estrategia.getStatus()).isEqualTo(StatusEstrategia.ATIVA);
		assertThat(estrategia.getHistorico()).last().extracting(HistoricoEstrategia::getAcao)
				.isEqualTo(AcaoHistoricoEstrategia.ATUALIZADA);
		assertThat(estrategia.getHistorico()).last().extracting(HistoricoEstrategia::getTitulo)
				.isEqualTo("Novo título");
	}

	@Test
	void ativacaoRegistraHistorico() {
		Estrategia estrategia = estrategia(StatusEstrategia.RASCUNHO);
		when(estrategiaRepository.findById("estrategia-id")).thenReturn(Optional.of(estrategia));
		when(estrategiaRepository.save(estrategia)).thenReturn(estrategia);

		EstrategiaResponse response = estrategiaService.ativar("estrategia-id", USUARIO_ID);

		assertThat(response.status()).isEqualTo(StatusEstrategia.ATIVA);
		assertThat(estrategia.getHistorico()).last().extracting(HistoricoEstrategia::getAcao)
				.isEqualTo(AcaoHistoricoEstrategia.ATIVADA);
	}

	@Test
	void desativacaoRegistraHistorico() {
		Estrategia estrategia = estrategia(StatusEstrategia.ATIVA);
		when(estrategiaRepository.findById("estrategia-id")).thenReturn(Optional.of(estrategia));
		when(estrategiaRepository.save(estrategia)).thenReturn(estrategia);

		EstrategiaResponse response = estrategiaService.desativar("estrategia-id", USUARIO_ID);

		assertThat(response.status()).isEqualTo(StatusEstrategia.INATIVA);
		assertThat(estrategia.getHistorico()).last().extracting(HistoricoEstrategia::getAcao)
				.isEqualTo(AcaoHistoricoEstrategia.DESATIVADA);
	}

	@Test
	void arquivamentoELogicoERegistraHistorico() {
		Estrategia estrategia = estrategia(StatusEstrategia.ATIVA);
		when(estrategiaRepository.findById("estrategia-id")).thenReturn(Optional.of(estrategia));
		when(estrategiaRepository.save(estrategia)).thenReturn(estrategia);

		estrategiaService.arquivar("estrategia-id", USUARIO_ID);

		assertThat(estrategia.getStatus()).isEqualTo(StatusEstrategia.ARQUIVADA);
		assertThat(estrategia.getHistorico()).last().extracting(HistoricoEstrategia::getAcao)
				.isEqualTo(AcaoHistoricoEstrategia.ARQUIVADA);
		verify(estrategiaRepository, never()).delete(any());
	}

	@Test
	void estrategiaArquivadaNaoPodeSerAlterada() {
		Estrategia estrategia = estrategia(StatusEstrategia.ARQUIVADA);
		when(estrategiaRepository.findById("estrategia-id")).thenReturn(Optional.of(estrategia));
		AtualizarEstrategiaRequest request = new AtualizarEstrategiaRequest(
				"Novo título", "Nova descrição completa", LocalDate.now(), "Categoria", "Campanha");

		assertThatThrownBy(() -> estrategiaService.atualizar("estrategia-id", request, USUARIO_ID))
				.isInstanceOf(OperacaoEstrategiaInvalidaException.class);
		assertThatThrownBy(() -> estrategiaService.ativar("estrategia-id", USUARIO_ID))
				.isInstanceOf(OperacaoEstrategiaInvalidaException.class);
		assertThatThrownBy(() -> estrategiaService.desativar("estrategia-id", USUARIO_ID))
				.isInstanceOf(OperacaoEstrategiaInvalidaException.class);
		verify(estrategiaRepository, never()).save(any());
	}

	@Test
	void ativacaoJaAtivaEIdempotente() {
		Estrategia estrategia = estrategia(StatusEstrategia.ATIVA);
		when(estrategiaRepository.findById("estrategia-id")).thenReturn(Optional.of(estrategia));

		estrategiaService.ativar("estrategia-id", USUARIO_ID);

		assertThat(estrategia.getHistorico()).isEmpty();
		verify(estrategiaRepository, never()).save(any());
	}

	@Test
	void desativacaoJaInativaEIdempotente() {
		Estrategia estrategia = estrategia(StatusEstrategia.INATIVA);
		when(estrategiaRepository.findById("estrategia-id")).thenReturn(Optional.of(estrategia));

		estrategiaService.desativar("estrategia-id", USUARIO_ID);

		assertThat(estrategia.getHistorico()).isEmpty();
		verify(estrategiaRepository, never()).save(any());
	}

	@Test
	void listagemAplicaPaginacaoOrdenacaoEFiltros() {
		when(mongoTemplate.count(any(Query.class), eq(Estrategia.class))).thenReturn(21L);
		when(mongoTemplate.find(any(Query.class), eq(Estrategia.class)))
				.thenReturn(List.of(estrategia(StatusEstrategia.ATIVA)));

		PaginaResponse<EstrategiaResponse> response = estrategiaService.listar(
				StatusEstrategia.ATIVA, " Inovação ", " Campanha anual ", 1, 20, USUARIO_ID);

		ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
		verify(mongoTemplate).find(captor.capture(), eq(Estrategia.class));
		Document filtros = captor.getValue().getQueryObject();
		assertThat(filtros).containsKeys("status", "categoria", "campanha");
		assertThat(captor.getValue().getSortObject()).containsEntry("data", -1);
		assertThat(response.pagina()).isEqualTo(1);
		assertThat(response.totalElementos()).isEqualTo(21);
		assertThat(response.totalPaginas()).isEqualTo(2);
		assertThat(response.conteudo()).hasSize(1);
	}

	@Test
	void consultaDeAtivasRetornaSomenteStatusAtivo() {
		when(estrategiaRepository.findByStatusOrderByDataDesc(StatusEstrategia.ATIVA))
				.thenReturn(List.of(estrategia(StatusEstrategia.ATIVA)));

		List<EstrategiaResponse> response = estrategiaService.listarAtivas(USUARIO_ID);

		assertThat(response).singleElement().extracting(EstrategiaResponse::status)
				.isEqualTo(StatusEstrategia.ATIVA);
	}

	@Test
	void historicoEOrdenadoDoMaisRecenteParaOMaisAntigo() {
		Estrategia estrategia = estrategia(StatusEstrategia.ATIVA);
		estrategia.setHistorico(List.of(
				historico(AcaoHistoricoEstrategia.CRIADA, Instant.parse("2027-01-01T10:00:00Z")),
				historico(AcaoHistoricoEstrategia.ATIVADA, Instant.parse("2027-01-02T10:00:00Z"))));
		when(estrategiaRepository.findById("estrategia-id")).thenReturn(Optional.of(estrategia));

		List<HistoricoEstrategiaResponse> response = estrategiaService.consultarHistorico("estrategia-id", USUARIO_ID);

		assertThat(response).extracting(HistoricoEstrategiaResponse::acao)
				.containsExactly(AcaoHistoricoEstrategia.ATIVADA, AcaoHistoricoEstrategia.CRIADA);
	}

	@Test
	void estrategiaInexistenteRetornaNaoEncontrada() {
		when(estrategiaRepository.findById("inexistente")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> estrategiaService.buscarPorId("inexistente", USUARIO_ID))
				.isInstanceOf(EstrategiaNaoEncontradaException.class);
	}

	private Estrategia estrategia(StatusEstrategia status) {
		Estrategia estrategia = new Estrategia();
		estrategia.setId("estrategia-id");
		estrategia.setTitulo("Estratégia 2027");
		estrategia.setDescricao("Descrição completa da estratégia");
		estrategia.setData(LocalDate.of(2027, 1, 10));
		estrategia.setCategoria("Inovação");
		estrategia.setCampanha("Campanha anual");
		estrategia.setStatus(status);
		estrategia.setCriadoPorId(USUARIO_ID);
		estrategia.setAtualizadoPorId(USUARIO_ID);
		estrategia.setCriadoEm(Instant.parse("2027-01-01T10:00:00Z"));
		estrategia.setAtualizadoEm(Instant.parse("2027-01-01T10:00:00Z"));
		estrategia.setVersao(0L);
		return estrategia;
	}

	private HistoricoEstrategia historico(AcaoHistoricoEstrategia acao, Instant dataHora) {
		return new HistoricoEstrategia(
				"historico-id",
				acao,
				dataHora,
				USUARIO_ID,
				"Estratégia 2027",
				"Descrição completa da estratégia",
				LocalDate.of(2027, 1, 10),
				"Inovação",
				"Campanha anual",
				StatusEstrategia.ATIVA);
	}
}
