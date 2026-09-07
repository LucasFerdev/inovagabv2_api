package br.com.inovagab.api.dto;

import java.util.List;

public record PaginaResponse<T>(
		List<T> conteudo,
		int pagina,
		int tamanho,
		long totalElementos,
		int totalPaginas,
		boolean primeira,
		boolean ultima) {

	public static <T> PaginaResponse<T> de(List<T> conteudo, int pagina, int tamanho, long totalElementos) {
		int totalPaginas = (int) Math.ceil((double) totalElementos / tamanho);
		return new PaginaResponse<>(
				conteudo,
				pagina,
				tamanho,
				totalElementos,
				totalPaginas,
				pagina == 0,
				totalPaginas == 0 || pagina >= totalPaginas - 1);
	}
}
