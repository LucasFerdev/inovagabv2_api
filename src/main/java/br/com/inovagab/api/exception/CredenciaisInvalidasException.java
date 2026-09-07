package br.com.inovagab.api.exception;

public class CredenciaisInvalidasException extends RuntimeException {

	public CredenciaisInvalidasException() {
		super("E-mail ou senha inválidos");
	}
}
