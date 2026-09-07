package br.com.inovagab.api.exception;

public class UsuarioInativoException extends RuntimeException {

	public UsuarioInativoException() {
		super("Usuário inativo");
	}
}
