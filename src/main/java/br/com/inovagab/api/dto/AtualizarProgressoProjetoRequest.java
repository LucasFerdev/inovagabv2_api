package br.com.inovagab.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import br.com.inovagab.api.model.EtapaProjeto;
import br.com.inovagab.api.model.StatusProjeto;

public record AtualizarProgressoProjetoRequest(
		@NotNull(message = "A etapa é obrigatória") EtapaProjeto etapa,
		@NotNull(message = "O status é obrigatório") StatusProjeto status,
		@NotNull(message = "O percentual de progresso é obrigatório") @Min(0) @Max(100) Integer percentualProgresso,
		@Size(max = 1000) String justificativa) {

	public AtualizarProgressoProjetoRequest {
		justificativa = justificativa == null ? null : justificativa.strip();
	}
}
