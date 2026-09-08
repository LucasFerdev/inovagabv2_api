package br.com.inovagab.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AtualizarProjetoRequest(
		@NotBlank(message = "O nome é obrigatório") @Size(min = 3, max = 120) String nome,
		@NotBlank(message = "A descrição é obrigatória") @Size(min = 10, max = 2000) String descricao,
		@NotBlank(message = "A estratégia é obrigatória") String estrategiaId,
		@Size(min = 1, message = "A ideia de origem é inválida") String ideiaOrigemId,
		@NotNull(message = "O investimento é obrigatório") @DecimalMin(value = "0.00") BigDecimal investimento,
		@NotNull(message = "O prazo é obrigatório") LocalDate prazo) {

	public AtualizarProjetoRequest {
		nome = normalizar(nome); descricao = normalizar(descricao); estrategiaId = normalizar(estrategiaId);
		ideiaOrigemId = normalizar(ideiaOrigemId);
	}

	private static String normalizar(String valor) { return valor == null ? null : valor.strip(); }
}
