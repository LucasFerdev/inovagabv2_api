package br.com.inovagab.api.dto;

import java.time.Instant;
import java.time.LocalDate;

import br.com.inovagab.api.model.AcaoHistoricoEstrategia;
import br.com.inovagab.api.model.HistoricoEstrategia;
import br.com.inovagab.api.model.StatusEstrategia;

public record HistoricoEstrategiaResponse(
		String id,
		AcaoHistoricoEstrategia acao,
		Instant dataHora,
		String usuarioId,
		String titulo,
		String descricao,
		LocalDate data,
		String categoria,
		String campanha,
		StatusEstrategia status) {

	public static HistoricoEstrategiaResponse de(HistoricoEstrategia historico) {
		return new HistoricoEstrategiaResponse(
				historico.getId(),
				historico.getAcao(),
				historico.getDataHora(),
				historico.getUsuarioId(),
				historico.getTitulo(),
				historico.getDescricao(),
				historico.getData(),
				historico.getCategoria(),
				historico.getCampanha(),
				historico.getStatus());
	}
}
