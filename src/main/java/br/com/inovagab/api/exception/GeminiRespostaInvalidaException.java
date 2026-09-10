package br.com.inovagab.api.exception;

public class GeminiRespostaInvalidaException extends RuntimeException {

	public GeminiRespostaInvalidaException() {
		super("Resposta inválida do serviço de inteligência artificial");
	}
}
