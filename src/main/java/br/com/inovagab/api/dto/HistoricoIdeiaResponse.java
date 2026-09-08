package br.com.inovagab.api.dto;

import java.time.Instant;

import br.com.inovagab.api.model.AcaoHistoricoIdeia;
import br.com.inovagab.api.model.HistoricoIdeia;
import br.com.inovagab.api.model.StatusIdeia;

public record HistoricoIdeiaResponse(
		String id,
		AcaoHistoricoIdeia acao,
		Instant dataHora,
		String usuarioId,
		String titulo,
		String problema,
		String solucaoProposta,
		String beneficiosEsperados,
		String categoria,
		String estrategiaId,
		StatusIdeia status,
		Integer prioridade,
		String justificativaAvaliacao) {

	public static HistoricoIdeiaResponse de(HistoricoIdeia historico) {
		return new HistoricoIdeiaResponse(historico.getId(), historico.getAcao(), historico.getDataHora(),
				historico.getUsuarioId(), historico.getTitulo(), historico.getProblema(), historico.getSolucaoProposta(),
				historico.getBeneficiosEsperados(), historico.getCategoria(), historico.getEstrategiaId(),
				historico.getStatus(), historico.getPrioridade(), historico.getJustificativaAvaliacao());
	}
}
