package br.com.inovagab.api.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CadastroPropertiesTest {

	@Test
	void codigosIguaisENaoVaziosSaoRejeitadosSemExporValor() {
		String codigo = "codigo-repetido-teste";

		assertThatThrownBy(() -> new CadastroProperties(codigo, codigo))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Os códigos de acesso de Gestor e Liderança devem ser diferentes")
				.hasMessageNotContaining(codigo);
	}
}
