package br.com.inovagab.api.dto;

import java.time.Instant;
import java.util.List;

import br.com.inovagab.api.model.AnaliseIaIdeia;

public record AnaliseIaIdeiaResponse(
		String ideiaId,
		Integer pontuacaoGeral,
		Integer prioridadeSugerida,
		String resumoExecutivo,
		List<String> pontosFortes,
		List<String> riscos,
		List<String> recomendacoes,
		String modelo,
		Instant geradoEm,
		String aviso) {

	public static AnaliseIaIdeiaResponse de(AnaliseIaIdeia analise) {
		return new AnaliseIaIdeiaResponse(analise.getIdeiaId(), analise.getPontuacaoGeral(),
				analise.getPrioridadeSugerida(), analise.getResumoExecutivo(), List.copyOf(analise.getPontosFortes()),
				List.copyOf(analise.getRiscos()), List.copyOf(analise.getRecomendacoes()), analise.getModelo(),
				analise.getGeradoEm(), analise.getAviso());
	}
}
