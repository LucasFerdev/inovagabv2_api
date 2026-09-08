package br.com.inovagab.api.dto;

import java.math.BigDecimal;

import br.com.inovagab.api.model.StatusEstrategia;

public record DashboardEstrategiaResponse(
		String estrategiaId,
		String titulo,
		StatusEstrategia status,
		long quantidadeIdeias,
		long ideiasAprovadas,
		long quantidadeProjetos,
		BigDecimal investimento,
		BigDecimal retorno,
		BigDecimal lucro,
		BigDecimal roiPercentual,
		BigDecimal progressoMedio,
		BigDecimal produtividadeMedia,
		long projetosAtrasados) {
}
