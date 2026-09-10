package br.com.inovagab.api.exception;

public class GeminiLimiteExcedidoException extends RuntimeException {

	public GeminiLimiteExcedidoException() {
		super("Limite temporário do serviço de inteligência artificial atingido");
	}
}
