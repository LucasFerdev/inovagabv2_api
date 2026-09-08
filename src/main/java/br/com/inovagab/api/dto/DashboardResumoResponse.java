package br.com.inovagab.api.dto;

import java.math.BigDecimal;
import java.util.Map;

import br.com.inovagab.api.model.StatusIdeia;
import br.com.inovagab.api.model.StatusProjeto;

public record DashboardResumoResponse(
		long totalProjetos,
		long totalPlanejado,
		long totalEmAndamento,
		long totalPausado,
		long totalConcluido,
		long totalCancelado,
		BigDecimal investimentoTotal,
		BigDecimal retornoFinanceiroTotal,
		BigDecimal lucroObtido,
		BigDecimal roiPercentual,
		BigDecimal progressoMedio,
		BigDecimal ganhoMedioProdutividade,
		long projetosAtrasados,
		long ideiasEnviadas,
		long ideiasEmAnalise,
		long ideiasAprovadas,
		long ideiasRejeitadas,
		Map<StatusProjeto, Long> projetosPorStatus,
		Map<StatusIdeia, Long> ideiasPorStatus) {
}
