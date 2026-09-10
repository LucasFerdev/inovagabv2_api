package br.com.inovagab.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.inovagab.api.dto.AnaliseIaIdeiaResponse;
import br.com.inovagab.api.exception.GeminiRespostaInvalidaException;
import br.com.inovagab.api.exception.IdeiaNaoEncontradaException;
import br.com.inovagab.api.exception.OperacaoIdeiaInvalidaException;
import br.com.inovagab.api.ia.GeminiClient;
import br.com.inovagab.api.ia.GeminiPrompt;
import br.com.inovagab.api.ia.GeminiResultado;
import br.com.inovagab.api.model.AcaoHistoricoIdeia;
import br.com.inovagab.api.model.AnaliseIaIdeia;
import br.com.inovagab.api.model.Estrategia;
import br.com.inovagab.api.model.Ideia;
import br.com.inovagab.api.model.StatusIdeia;
import br.com.inovagab.api.model.Usuario;
import br.com.inovagab.api.repository.EstrategiaRepository;
import br.com.inovagab.api.repository.IdeiaRepository;

@ExtendWith(MockitoExtension.class)
class AnaliseIaServiceTest {

	@Mock private IdeiaRepository ideiaRepository;
	@Mock private EstrategiaRepository estrategiaRepository;
	@Mock private UsuarioAutenticadoService usuarioAutenticadoService;
	@Mock private GeminiClient geminiClient;
	private AnaliseIaService service;

	@BeforeEach
	void configurar() {
		service = new AnaliseIaService(ideiaRepository, estrategiaRepository, usuarioAutenticadoService, geminiClient);
		lenient().when(usuarioAutenticadoService.buscarAtivo("gestor-id")).thenReturn(new Usuario());
	}

	@Test
	void gestorExecutaAnaliseValidaSemEnviarDadosPessoais() {
		Ideia ideia = ideia();
		when(ideiaRepository.findById("ideia-id")).thenReturn(Optional.of(ideia));
		when(estrategiaRepository.findById("estrategia-id")).thenReturn(Optional.of(estrategia()));
		when(geminiClient.analisar(any())).thenReturn(resultado(85));
		when(ideiaRepository.save(ideia)).thenReturn(ideia);

		AnaliseIaIdeiaResponse response = service.analisar("ideia-id", false, "gestor-id");

		assertThat(response.pontuacaoGeral()).isEqualTo(85);
		assertThat(response.aviso()).isEqualTo(AnaliseIaIdeia.AVISO_PADRAO);
		assertThat(ideia.getStatus()).isEqualTo(StatusIdeia.EM_ANALISE);
		assertThat(ideia.getPrioridade()).isNull();
		assertThat(ideia.getHistorico()).last().satisfies(item -> {
			assertThat(item.getAcao()).isEqualTo(AcaoHistoricoIdeia.ANALISADA_POR_IA);
			assertThat(item.getAnaliseIa().getPontuacaoGeral()).isEqualTo(85);
		});
		ArgumentCaptor<GeminiPrompt> captor = ArgumentCaptor.forClass(GeminiPrompt.class);
		verify(geminiClient).analisar(captor.capture());
		assertThat(captor.getValue().toString()).doesNotContain("gestor-id", "autor-id", "email", "empresa", "token");
	}

	@Test
	void ideiaInexistenteOuForaDeAnaliseERejeitada() {
		when(ideiaRepository.findById("inexistente")).thenReturn(Optional.empty());
		assertThatThrownBy(() -> service.analisar("inexistente", false, "gestor-id"))
				.isInstanceOf(IdeiaNaoEncontradaException.class);
		Ideia ideia = ideia();
		ideia.setStatus(StatusIdeia.ENVIADA);
		when(ideiaRepository.findById("ideia-id")).thenReturn(Optional.of(ideia));
		assertThatThrownBy(() -> service.analisar("ideia-id", false, "gestor-id"))
				.isInstanceOf(OperacaoIdeiaInvalidaException.class);
		verify(geminiClient, never()).analisar(any());
	}

	@Test
	void pontuacoesForaDosLimitesSaoRejeitadas() {
		prepararAnalise();
		when(geminiClient.analisar(any())).thenReturn(resultado(101));
		assertThatThrownBy(() -> service.analisar("ideia-id", false, "gestor-id"))
				.isInstanceOf(GeminiRespostaInvalidaException.class);
		when(geminiClient.analisar(any())).thenReturn(new GeminiResultado(80, 0, "Resumo", List.of(), List.of(),
				List.of(), "gemini-3.5-flash-lite"));
		assertThatThrownBy(() -> service.analisar("ideia-id", false, "gestor-id"))
				.isInstanceOf(GeminiRespostaInvalidaException.class);
	}

	@Test
	void respostaSemCamposObrigatoriosERejeitada() {
		prepararAnalise();
		when(geminiClient.analisar(any())).thenReturn(new GeminiResultado(80, 4, null, null, List.of(), List.of(), null));
		assertThatThrownBy(() -> service.analisar("ideia-id", false, "gestor-id"))
				.isInstanceOf(GeminiRespostaInvalidaException.class);
	}

	@Test
	void analiseArmazenadaNaoFazNovaChamada() {
		Ideia ideia = ideia();
		ideia.setAnaliseIa(analise(70));
		when(ideiaRepository.findById("ideia-id")).thenReturn(Optional.of(ideia));
		assertThat(service.analisar("ideia-id", false, "gestor-id").pontuacaoGeral()).isEqualTo(70);
		verify(geminiClient, never()).analisar(any());
		verify(ideiaRepository, never()).save(any());
	}

	@Test
	void recalculoSubstituiAtualEPreservaHistorico() {
		Ideia ideia = ideia();
		ideia.setAnaliseIa(analise(70));
		when(ideiaRepository.findById("ideia-id")).thenReturn(Optional.of(ideia));
		when(estrategiaRepository.findById("estrategia-id")).thenReturn(Optional.of(estrategia()));
		when(geminiClient.analisar(any())).thenReturn(resultado(80), resultado(90));
		when(ideiaRepository.save(ideia)).thenReturn(ideia);

		service.analisar("ideia-id", true, "gestor-id");
		service.analisar("ideia-id", true, "gestor-id");

		assertThat(ideia.getAnaliseIa().getPontuacaoGeral()).isEqualTo(90);
		assertThat(ideia.getHistorico()).extracting(item -> item.getAnaliseIa().getPontuacaoGeral())
				.containsExactly(80, 90);
		verify(geminiClient, times(2)).analisar(any());
	}

	@Test
	void consultaRetornaAnaliseSemChamarGemini() {
		Ideia ideia = ideia();
		ideia.setAnaliseIa(analise(88));
		when(ideiaRepository.findById("ideia-id")).thenReturn(Optional.of(ideia));
		assertThat(service.consultar("ideia-id", "gestor-id").pontuacaoGeral()).isEqualTo(88);
		verify(geminiClient, never()).analisar(any());
	}

	private void prepararAnalise() {
		when(ideiaRepository.findById("ideia-id")).thenReturn(Optional.of(ideia()));
		when(estrategiaRepository.findById("estrategia-id")).thenReturn(Optional.of(estrategia()));
	}

	private Ideia ideia() {
		Ideia ideia = new Ideia();
		ideia.setId("ideia-id"); ideia.setTitulo("Automação do atendimento");
		ideia.setProblema("Processo manual demorado"); ideia.setSolucaoProposta("Automatizar a triagem");
		ideia.setBeneficiosEsperados("Reduzir tempo e custos"); ideia.setCategoria("Automação");
		ideia.setEstrategiaId("estrategia-id"); ideia.setAutorId("autor-id"); ideia.setStatus(StatusIdeia.EM_ANALISE);
		return ideia;
	}

	private Estrategia estrategia() {
		Estrategia estrategia = new Estrategia();
		estrategia.setId("estrategia-id"); estrategia.setTitulo("Eficiência operacional");
		estrategia.setCategoria("Eficiência"); return estrategia;
	}

	private GeminiResultado resultado(int pontuacao) {
		return new GeminiResultado(pontuacao, 4, " Avaliação consultiva ", List.of(" Bom impacto "),
				List.of(" Risco de adoção "), List.of(" Realizar piloto "), "gemini-3.5-flash-lite");
	}

	private AnaliseIaIdeia analise(int pontuacao) {
		return new AnaliseIaIdeia("ideia-id", pontuacao, 4, "Resumo", List.of("Impacto"), List.of("Risco"),
				List.of("Piloto"), "gemini-3.5-flash-lite", Instant.now(), AnaliseIaIdeia.AVISO_PADRAO);
	}
}
