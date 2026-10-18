package br.com.inovagab.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistrarResultadosProjetoRequest(
		@NotNull(message = "O retorno financeiro é obrigatório")
		@DecimalMin(value = "0.00", message = "O retorno financeiro não pode ser negativo")
		@Digits(integer = 13, fraction = 2, message = "O retorno financeiro deve ter no máximo 13 dígitos inteiros e 2 casas decimais")
		@DecimalMax(value = "9999999999999.99", message = "O retorno financeiro não pode ser superior a 9999999999999.99")
		BigDecimal retornoFinanceiro,
		@NotNull(message = "O ganho de produtividade é obrigatório")
		@DecimalMin(value = "0.00", message = "O ganho de produtividade não pode ser negativo")
		@DecimalMax(value = "1000.00", message = "O ganho de produtividade não pode ser superior a 1000.00")
		BigDecimal ganhoProdutividadePercentual,
		@NotBlank(message = "O resultado é obrigatório") @Size(min = 3, max = 2000) String resultado) {

	public RegistrarResultadosProjetoRequest {
		resultado = resultado == null ? null : resultado.strip();
	}
}
