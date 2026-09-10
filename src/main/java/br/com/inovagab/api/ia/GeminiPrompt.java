package br.com.inovagab.api.ia;

public record GeminiPrompt(
		String titulo,
		String problema,
		String solucaoProposta,
		String beneficiosEsperados,
		String categoria,
		String tituloEstrategia,
		String categoriaEstrategia) {
}
