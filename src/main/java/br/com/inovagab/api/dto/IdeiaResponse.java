package br.com.inovagab.api.dto;

import java.time.Instant;

import br.com.inovagab.api.model.Ideia;
import br.com.inovagab.api.model.StatusIdeia;

public record IdeiaResponse(
		String id,
		String titulo,
		String problema,
		String solucaoProposta,
		String beneficiosEsperados,
		String categoria,
		String estrategiaId,
		String autorId,
		StatusIdeia status,
		Integer prioridade,
		String justificativaAvaliacao,
		String avaliadoPorId,
		Instant avaliadoEm,
		Instant criadoEm,
		Instant atualizadoEm,
		Long versao) {

	public static IdeiaResponse de(Ideia ideia) {
		return new IdeiaResponse(ideia.getId(), ideia.getTitulo(), ideia.getProblema(), ideia.getSolucaoProposta(),
				ideia.getBeneficiosEsperados(), ideia.getCategoria(), ideia.getEstrategiaId(), ideia.getAutorId(),
				ideia.getStatus(), ideia.getPrioridade(), ideia.getJustificativaAvaliacao(), ideia.getAvaliadoPorId(),
				ideia.getAvaliadoEm(), ideia.getCriadoEm(), ideia.getAtualizadoEm(), ideia.getVersao());
	}
}
