package br.com.inovagab.api.dto;

import java.time.Instant;

public record RespostaErro(Instant timestamp, int status, String erro, String mensagem, String path) {
}
