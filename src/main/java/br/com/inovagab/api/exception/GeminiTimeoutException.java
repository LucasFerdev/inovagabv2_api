package br.com.inovagab.api.exception;

public class GeminiTimeoutException extends RuntimeException {

	public GeminiTimeoutException() {
		super("O serviço de inteligência artificial excedeu o tempo de resposta");
	}
}
