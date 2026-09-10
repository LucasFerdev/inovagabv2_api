package br.com.inovagab.api.exception;

public class GeminiIndisponivelException extends RuntimeException {

	public GeminiIndisponivelException() {
		super("Serviço de inteligência artificial indisponível");
	}
}
