package br.com.inovagab.api.dto;

import java.time.Instant;
import java.time.LocalDate;

import br.com.inovagab.api.model.Estrategia;
import br.com.inovagab.api.model.StatusEstrategia;

public record EstrategiaResponse(
		String id,
		String titulo,
		String descricao,
		LocalDate data,
		String categoria,
		String campanha,
		StatusEstrategia status,
		String criadoPorId,
		String atualizadoPorId,
		Instant criadoEm,
		Instant atualizadoEm,
		Long versao) {

	public static EstrategiaResponse de(Estrategia estrategia) {
		return new EstrategiaResponse(
				estrategia.getId(),
				estrategia.getTitulo(),
				estrategia.getDescricao(),
				estrategia.getData(),
				estrategia.getCategoria(),
				estrategia.getCampanha(),
				estrategia.getStatus(),
				estrategia.getCriadoPorId(),
				estrategia.getAtualizadoPorId(),
				estrategia.getCriadoEm(),
				estrategia.getAtualizadoEm(),
				estrategia.getVersao());
	}
}
