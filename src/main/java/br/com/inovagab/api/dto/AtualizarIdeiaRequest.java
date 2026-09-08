package br.com.inovagab.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AtualizarIdeiaRequest(
		@NotBlank(message = "O título é obrigatório")
		@Size(min = 3, max = 120, message = "O título deve ter entre 3 e 120 caracteres") String titulo,
		@NotBlank(message = "O problema é obrigatório")
		@Size(min = 10, max = 2000, message = "O problema deve ter entre 10 e 2000 caracteres") String problema,
		@NotBlank(message = "A solução proposta é obrigatória")
		@Size(min = 10, max = 2000, message = "A solução proposta deve ter entre 10 e 2000 caracteres") String solucaoProposta,
		@NotBlank(message = "Os benefícios esperados são obrigatórios")
		@Size(min = 10, max = 2000, message = "Os benefícios esperados devem ter entre 10 e 2000 caracteres") String beneficiosEsperados,
		@NotBlank(message = "A categoria é obrigatória")
		@Size(min = 2, max = 80, message = "A categoria deve ter entre 2 e 80 caracteres") String categoria,
		@NotBlank(message = "A estratégia é obrigatória") String estrategiaId) {

	public AtualizarIdeiaRequest {
		titulo = normalizar(titulo);
		problema = normalizar(problema);
		solucaoProposta = normalizar(solucaoProposta);
		beneficiosEsperados = normalizar(beneficiosEsperados);
		categoria = normalizar(categoria);
		estrategiaId = normalizar(estrategiaId);
	}

	private static String normalizar(String valor) {
		return valor == null ? null : valor.strip();
	}
}
