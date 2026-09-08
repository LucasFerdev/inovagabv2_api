package br.com.inovagab.api.exception;

public class ProjetoNaoEncontradoException extends RuntimeException {

	public ProjetoNaoEncontradoException() {
		super("Projeto não encontrado");
	}
}
