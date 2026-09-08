package br.com.inovagab.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import br.com.inovagab.api.model.AcaoHistoricoProjeto;
import br.com.inovagab.api.model.EtapaProjeto;
import br.com.inovagab.api.model.HistoricoProjeto;
import br.com.inovagab.api.model.StatusProjeto;

public record HistoricoProjetoResponse(String id, AcaoHistoricoProjeto acao, Instant dataHora, String usuarioId,
		String nome, String descricao, String estrategiaId, String ideiaOrigemId, EtapaProjeto etapa,
		StatusProjeto status, Integer percentualProgresso, BigDecimal investimento, LocalDate prazo,
		BigDecimal retornoFinanceiro, BigDecimal ganhoProdutividadePercentual, String resultado, String justificativa) {

	public static HistoricoProjetoResponse de(HistoricoProjeto item) {
		return new HistoricoProjetoResponse(item.getId(), item.getAcao(), item.getDataHora(), item.getUsuarioId(),
				item.getNome(), item.getDescricao(), item.getEstrategiaId(), item.getIdeiaOrigemId(), item.getEtapa(),
				item.getStatus(), item.getPercentualProgresso(), item.getInvestimento(), item.getPrazo(),
				item.getRetornoFinanceiro(), item.getGanhoProdutividadePercentual(), item.getResultado(),
				item.getJustificativa());
	}
}
