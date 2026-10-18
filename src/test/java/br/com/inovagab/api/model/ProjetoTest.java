package br.com.inovagab.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class ProjetoTest {

	@Test
	void prazoOntemEmAndamentoEstaAtrasado() {
		LocalDate ontem = LocalDate.now(Projeto.FUSO).minusDays(1);
		assertThat(Projeto.atrasado(ontem, StatusProjeto.EM_ANDAMENTO)).isTrue();
	}

	@Test
	void prazoOntemConcluidoNaoEstaAtrasado() {
		LocalDate ontem = LocalDate.now(Projeto.FUSO).minusDays(1);
		assertThat(Projeto.atrasado(ontem, StatusProjeto.CONCLUIDO)).isFalse();
	}

	@Test
	void prazoOntemCanceladoNaoEstaAtrasado() {
		LocalDate ontem = LocalDate.now(Projeto.FUSO).minusDays(1);
		assertThat(Projeto.atrasado(ontem, StatusProjeto.CANCELADO)).isFalse();
	}

	@Test
	void prazoHojeNaoEstaAtrasado() {
		assertThat(Projeto.atrasado(LocalDate.now(Projeto.FUSO), StatusProjeto.EM_ANDAMENTO)).isFalse();
	}

	@Test
	void prazoNuloNaoEstaAtrasado() {
		assertThat(Projeto.atrasado(null, StatusProjeto.EM_ANDAMENTO)).isFalse();
	}
}
