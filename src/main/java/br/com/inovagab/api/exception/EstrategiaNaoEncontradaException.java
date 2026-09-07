package br.com.inovagab.api.exception;

public class EstrategiaNaoEncontradaException extends RuntimeException {

	public EstrategiaNaoEncontradaException() {
		super("Estratégia não encontrada");
	}
}
