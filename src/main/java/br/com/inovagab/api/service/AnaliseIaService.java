package br.com.inovagab.api.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import br.com.inovagab.api.dto.AnaliseIaIdeiaResponse;
import br.com.inovagab.api.exception.AnaliseIaNaoEncontradaException;
import br.com.inovagab.api.exception.EstrategiaNaoEncontradaException;
import br.com.inovagab.api.exception.GeminiRespostaInvalidaException;
import br.com.inovagab.api.exception.IdeiaNaoEncontradaException;
import br.com.inovagab.api.exception.OperacaoIdeiaInvalidaException;
import br.com.inovagab.api.ia.GeminiClient;
import br.com.inovagab.api.ia.GeminiPrompt;
import br.com.inovagab.api.ia.GeminiResultado;
import br.com.inovagab.api.model.AcaoHistoricoIdeia;
import br.com.inovagab.api.model.AnaliseIaIdeia;
import br.com.inovagab.api.model.Estrategia;
import br.com.inovagab.api.model.HistoricoIdeia;
import br.com.inovagab.api.model.Ideia;
import br.com.inovagab.api.model.StatusIdeia;
import br.com.inovagab.api.repository.EstrategiaRepository;
import br.com.inovagab.api.repository.IdeiaRepository;

@Service
public class AnaliseIaService {

	private static final int LIMITE_ITENS = 10;
	private final IdeiaRepository ideiaRepository;
	private final EstrategiaRepository estrategiaRepository;
	private final UsuarioAutenticadoService usuarioAutenticadoService;
	private final GeminiClient geminiClient;

	public AnaliseIaService(IdeiaRepository ideiaRepository, EstrategiaRepository estrategiaRepository,
			UsuarioAutenticadoService usuarioAutenticadoService, GeminiClient geminiClient) {
		this.ideiaRepository = ideiaRepository;
		this.estrategiaRepository = estrategiaRepository;
		this.usuarioAutenticadoService = usuarioAutenticadoService;
		this.geminiClient = geminiClient;
	}

	public AnaliseIaIdeiaResponse analisar(String ideiaId, boolean recalcular, String usuarioId) {
		usuarioAutenticadoService.buscarAtivo(usuarioId);
		Ideia ideia = buscarIdeia(ideiaId);
		if (ideia.getStatus() != StatusIdeia.EM_ANALISE) {
			throw new OperacaoIdeiaInvalidaException("A ideia deve estar em análise");
		}
		if (!recalcular && ideia.getAnaliseIa() != null) {
			return AnaliseIaIdeiaResponse.de(ideia.getAnaliseIa());
		}
		Estrategia estrategia = estrategiaRepository.findById(ideia.getEstrategiaId())
				.orElseThrow(EstrategiaNaoEncontradaException::new);
		GeminiResultado resultado = geminiClient.analisar(new GeminiPrompt(ideia.getTitulo(), ideia.getProblema(),
				ideia.getSolucaoProposta(), ideia.getBeneficiosEsperados(), ideia.getCategoria(), estrategia.getTitulo(),
				estrategia.getCategoria()));
		AnaliseIaIdeia analise = normalizarEValidar(ideiaId, resultado);
		ideia.setAnaliseIa(analise);
		registrarHistorico(ideia, usuarioId, analise);
		ideiaRepository.save(ideia);
		return AnaliseIaIdeiaResponse.de(analise);
	}

	public AnaliseIaIdeiaResponse consultar(String ideiaId, String usuarioId) {
		usuarioAutenticadoService.buscarAtivo(usuarioId);
		Ideia ideia = buscarIdeia(ideiaId);
		if (ideia.getAnaliseIa() == null) {
			throw new AnaliseIaNaoEncontradaException();
		}
		return AnaliseIaIdeiaResponse.de(ideia.getAnaliseIa());
	}

	private AnaliseIaIdeia normalizarEValidar(String ideiaId, GeminiResultado resultado) {
		if (resultado == null || resultado.pontuacaoGeral() == null || resultado.pontuacaoGeral() < 0
				|| resultado.pontuacaoGeral() > 100 || resultado.prioridadeSugerida() == null
				|| resultado.prioridadeSugerida() < 1 || resultado.prioridadeSugerida() > 5) {
			throw new GeminiRespostaInvalidaException();
		}
		String resumo = textoObrigatorio(resultado.resumoExecutivo(), 3000);
		String modelo = textoObrigatorio(resultado.modelo(), 200);
		List<String> pontosFortes = listaValida(resultado.pontosFortes());
		List<String> riscos = listaValida(resultado.riscos());
		List<String> recomendacoes = listaValida(resultado.recomendacoes());
		return new AnaliseIaIdeia(ideiaId, resultado.pontuacaoGeral(), resultado.prioridadeSugerida(), resumo,
				pontosFortes, riscos, recomendacoes, modelo, Instant.now(), AnaliseIaIdeia.AVISO_PADRAO);
	}

	private String textoObrigatorio(String valor, int tamanhoMaximo) {
		if (!StringUtils.hasText(valor)) throw new GeminiRespostaInvalidaException();
		String normalizado = valor.strip();
		if (normalizado.length() > tamanhoMaximo) throw new GeminiRespostaInvalidaException();
		return normalizado;
	}

	private List<String> listaValida(List<String> valores) {
		if (valores == null || valores.size() > LIMITE_ITENS) throw new GeminiRespostaInvalidaException();
		return valores.stream().map(valor -> textoObrigatorio(valor, 1000)).toList();
	}

	private Ideia buscarIdeia(String ideiaId) {
		return ideiaRepository.findById(ideiaId).orElseThrow(IdeiaNaoEncontradaException::new);
	}

	private void registrarHistorico(Ideia ideia, String usuarioId, AnaliseIaIdeia analise) {
		ideia.adicionarHistorico(new HistoricoIdeia(UUID.randomUUID().toString(), AcaoHistoricoIdeia.ANALISADA_POR_IA,
				Instant.now(), usuarioId, ideia.getTitulo(), ideia.getProblema(), ideia.getSolucaoProposta(),
				ideia.getBeneficiosEsperados(), ideia.getCategoria(), ideia.getEstrategiaId(), ideia.getStatus(),
				ideia.getPrioridade(), ideia.getJustificativaAvaliacao(), copiarAnalise(analise)));
	}

	private AnaliseIaIdeia copiarAnalise(AnaliseIaIdeia analise) {
		return new AnaliseIaIdeia(analise.getIdeiaId(), analise.getPontuacaoGeral(), analise.getPrioridadeSugerida(),
				analise.getResumoExecutivo(), analise.getPontosFortes(), analise.getRiscos(), analise.getRecomendacoes(),
				analise.getModelo(), analise.getGeradoEm(), analise.getAviso());
	}
}
