package br.com.inovagab.api.dto;

public record RankingColaboradorResponse(
		int posicao,
		String nome,
		String empresa,
		long ideiasAprovadas,
		long ideiasImplementadas,
		MedalhaRanking medalha) {
}
