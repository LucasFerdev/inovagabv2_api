package br.com.inovagab.api.ia;

import java.util.List;

public record GeminiResultado(
		Integer pontuacaoGeral,
		Integer prioridadeSugerida,
		String resumoExecutivo,
		List<String> pontosFortes,
		List<String> riscos,
		List<String> recomendacoes,
		String modelo) {
}
