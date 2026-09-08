package br.com.inovagab.api.exception;

public class IdeiaNaoEncontradaException extends RuntimeException {

	public IdeiaNaoEncontradaException() {
		super("Ideia não encontrada");
	}
}
