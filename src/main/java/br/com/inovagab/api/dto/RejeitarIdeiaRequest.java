package br.com.inovagab.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejeitarIdeiaRequest(
		@NotBlank(message = "A justificativa é obrigatória")
		@Size(min = 3, max = 1000, message = "A justificativa deve ter entre 3 e 1000 caracteres") String justificativa) {

	public RejeitarIdeiaRequest {
		justificativa = justificativa == null ? null : justificativa.strip();
	}
}
