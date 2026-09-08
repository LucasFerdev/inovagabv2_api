package br.com.inovagab.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.inovagab.api.model.EtapaProjeto;
import br.com.inovagab.api.model.StatusProjeto;

public record DashboardProjetoResponse(
		String id,
		String nome,
		String descricao,
		DashboardEstrategiaReferenciaResponse estrategia,
		DashboardIdeiaReferenciaResponse ideiaOrigem,
		EtapaProjeto etapa,
		StatusProjeto status,
		Integer percentualProgresso,
		BigDecimal investimento,
		BigDecimal retornoFinanceiro,
		BigDecimal lucro,
		BigDecimal roiPercentual,
		LocalDate prazo,
		boolean atrasado,
		BigDecimal ganhoProdutividadePercentual,
		String resultado) {
}
