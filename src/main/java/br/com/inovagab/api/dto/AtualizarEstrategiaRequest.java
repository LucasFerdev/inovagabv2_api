package br.com.inovagab.api.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AtualizarEstrategiaRequest(
		@NotBlank(message = "O título é obrigatório")
		@Size(min = 3, max = 120, message = "O título deve ter entre 3 e 120 caracteres") String titulo,
		@NotBlank(message = "A descrição é obrigatória")
		@Size(min = 10, max = 2000, message = "A descrição deve ter entre 10 e 2000 caracteres") String descricao,
		@NotNull(message = "A data é obrigatória") LocalDate data,
		@NotBlank(message = "A categoria é obrigatória")
		@Size(min = 2, max = 80, message = "A categoria deve ter entre 2 e 80 caracteres") String categoria,
		@NotBlank(message = "A campanha é obrigatória")
		@Size(min = 2, max = 120, message = "A campanha deve ter entre 2 e 120 caracteres") String campanha) {

	public AtualizarEstrategiaRequest {
		titulo = normalizar(titulo);
		descricao = normalizar(descricao);
		categoria = normalizar(categoria);
		campanha = normalizar(campanha);
	}

	private static String normalizar(String valor) {
		return valor == null ? null : valor.strip();
	}
}
