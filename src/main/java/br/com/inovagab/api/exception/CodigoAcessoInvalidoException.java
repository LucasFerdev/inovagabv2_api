package br.com.inovagab.api.exception;

public class CodigoAcessoInvalidoException extends RuntimeException {

	public CodigoAcessoInvalidoException() {
		super("Código de acesso inválido");
	}
}
