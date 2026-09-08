package br.com.inovagab.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import br.com.inovagab.api.model.EtapaProjeto;
import br.com.inovagab.api.model.Projeto;
import br.com.inovagab.api.model.StatusProjeto;

public record ProjetoResponse(String id, String nome, String descricao, String estrategiaId, String ideiaOrigemId,
		EtapaProjeto etapa, StatusProjeto status, Integer percentualProgresso, BigDecimal investimento, LocalDate prazo,
		BigDecimal retornoFinanceiro, BigDecimal ganhoProdutividadePercentual, String resultado,
		String gestorResponsavelId, Instant criadoEm, Instant atualizadoEm, Long versao) {

	public static ProjetoResponse de(Projeto projeto) {
		return new ProjetoResponse(projeto.getId(), projeto.getNome(), projeto.getDescricao(), projeto.getEstrategiaId(),
				projeto.getIdeiaOrigemId(), projeto.getEtapa(), projeto.getStatus(), projeto.getPercentualProgresso(),
				projeto.getInvestimento(), projeto.getPrazo(), projeto.getRetornoFinanceiro(),
				projeto.getGanhoProdutividadePercentual(), projeto.getResultado(), projeto.getGestorResponsavelId(),
				projeto.getCriadoEm(), projeto.getAtualizadoEm(), projeto.getVersao());
	}
}
