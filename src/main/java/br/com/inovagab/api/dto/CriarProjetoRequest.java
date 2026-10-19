package br.com.inovagab.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CriarProjetoRequest(
		@NotBlank(message = "O nome é obrigatório")
		@Size(min = 3, max = 120, message = "O nome deve ter entre 3 e 120 caracteres") String nome,
		@NotBlank(message = "A descrição é obrigatória")
		@Size(min = 10, max = 2000, message = "A descrição deve ter entre 10 e 2000 caracteres") String descricao,
		@NotBlank(message = "A estratégia é obrigatória") String estrategiaId,
		@Size(min = 1, message = "A ideia de origem é inválida") String ideiaOrigemId,
		@NotNull(message = "O investimento é obrigatório")
		@DecimalMin(value = "0.00", message = "O investimento não pode ser negativo")
		@Digits(integer = 13, fraction = 2, message = "O investimento deve ter no máximo 13 dígitos inteiros e 2 casas decimais")
		@DecimalMax(value = "9999999999999.99", message = "O investimento não pode ser superior a 9999999999999.99")
		BigDecimal investimento,
		@NotNull(message = "O prazo é obrigatório") LocalDate prazo) {

	public CriarProjetoRequest {
		nome = normalizar(nome); descricao = normalizar(descricao); estrategiaId = normalizar(estrategiaId);
		ideiaOrigemId = normalizar(ideiaOrigemId);
	}

	private static String normalizar(String valor) { return valor == null ? null : valor.strip(); }
}
