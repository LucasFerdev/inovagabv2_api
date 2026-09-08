package br.com.inovagab.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistrarResultadosProjetoRequest(
		@NotNull(message = "O retorno financeiro é obrigatório") @DecimalMin("0.00") BigDecimal retornoFinanceiro,
		@NotNull(message = "O ganho de produtividade é obrigatório") @DecimalMin("0.00") BigDecimal ganhoProdutividadePercentual,
		@NotBlank(message = "O resultado é obrigatório") @Size(min = 3, max = 2000) String resultado) {

	public RegistrarResultadosProjetoRequest {
		resultado = resultado == null ? null : resultado.strip();
	}
}
