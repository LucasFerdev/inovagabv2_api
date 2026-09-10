package br.com.inovagab.api.exception;

public class AnaliseIaNaoEncontradaException extends RuntimeException {

	public AnaliseIaNaoEncontradaException() {
		super("Análise de inteligência artificial não encontrada");
	}
}
