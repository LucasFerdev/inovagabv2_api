package br.com.inovagab.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PriorizarIdeiaRequest(
		@NotNull(message = "A prioridade é obrigatória")
		@Min(value = 1, message = "A prioridade mínima é 1")
		@Max(value = 5, message = "A prioridade máxima é 5") Integer prioridade,
		@Size(max = 1000, message = "A justificativa deve ter no máximo 1000 caracteres") String justificativa) {

	public PriorizarIdeiaRequest {
		justificativa = justificativa == null ? null : justificativa.strip();
	}
}
