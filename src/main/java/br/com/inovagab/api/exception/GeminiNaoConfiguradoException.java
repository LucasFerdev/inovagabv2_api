package br.com.inovagab.api.exception;

public class GeminiNaoConfiguradoException extends RuntimeException {

	public GeminiNaoConfiguradoException() {
		super("Serviço de inteligência artificial não configurado");
	}
}
