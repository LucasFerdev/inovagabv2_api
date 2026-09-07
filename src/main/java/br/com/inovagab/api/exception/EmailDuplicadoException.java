package br.com.inovagab.api.exception;

public class EmailDuplicadoException extends RuntimeException {

	public EmailDuplicadoException() {
		super("Já existe um usuário cadastrado com este e-mail");
	}
}
