package br.com.inovagab.api.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import br.com.inovagab.api.config.GeminiProperties;
import br.com.inovagab.api.exception.GeminiIndisponivelException;
import br.com.inovagab.api.exception.GeminiLimiteExcedidoException;
import br.com.inovagab.api.exception.GeminiNaoConfiguradoException;
import br.com.inovagab.api.exception.GeminiRespostaInvalidaException;
import br.com.inovagab.api.exception.GeminiTimeoutException;
import tools.jackson.databind.ObjectMapper;

class GeminiRestClientTest {

	private HttpClient httpClient;
	private GeminiRestClient client;

	@BeforeEach
	void configurar() {
		httpClient = mock(HttpClient.class);
		client = new GeminiRestClient(new GeminiProperties("chave-ficticia", "gemini-3.5-flash-lite",
				URI.create("https://generativelanguage.googleapis.com"), 20), new ObjectMapper(), httpClient);
	}

	@Test
	void respostaValidaDoGeminiEConvertida() throws Exception {
		responder(200, respostaValida());
		GeminiResultado resultado = client.analisar(prompt());
		assertThat(resultado.pontuacaoGeral()).isEqualTo(87);
		assertThat(resultado.prioridadeSugerida()).isEqualTo(5);
		assertThat(resultado.modelo()).isEqualTo("gemini-3.5-flash-lite");
		ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
		verify(httpClient).send(captor.capture(), any());
		assertThat(captor.getValue().uri().toString()).endsWith("/v1beta/interactions");
		assertThat(captor.getValue().headers().firstValue("x-goog-api-key")).contains("chave-ficticia");
		assertThat(captor.getValue().timeout()).contains(Duration.ofSeconds(20));
	}

	@Test
	void jsonInvalidoERespostaSemCamposSaoRejeitados() throws Exception {
		responder(200, "{json-invalido");
		assertThatThrownBy(() -> client.analisar(prompt())).isInstanceOf(GeminiRespostaInvalidaException.class);
		responder(200, "{\"status\":\"completed\",\"model\":\"gemini\",\"steps\":[]}");
		assertThatThrownBy(() -> client.analisar(prompt())).isInstanceOf(GeminiRespostaInvalidaException.class);
	}

	@Test
	void chaveAusenteRetornaFalhaDeConfiguracao() {
		GeminiRestClient semChave = new GeminiRestClient(new GeminiProperties("", "gemini-3.5-flash-lite",
				URI.create("https://generativelanguage.googleapis.com"), 20), new ObjectMapper(), httpClient);
		assertThatThrownBy(() -> semChave.analisar(prompt())).isInstanceOf(GeminiNaoConfiguradoException.class);
	}

	@Test
	void timeoutERateLimitSaoMapeados() throws Exception {
		when(httpClient.send(any(), any())).thenThrow(new HttpTimeoutException("timeout"));
		assertThatThrownBy(() -> client.analisar(prompt())).isInstanceOf(GeminiTimeoutException.class);
		responder(429, "{}");
		assertThatThrownBy(() -> client.analisar(prompt())).isInstanceOf(GeminiLimiteExcedidoException.class);
	}

	@Test
	void indisponibilidadeExternaEMapeada() throws Exception {
		responder(503, "{}");
		assertThatThrownBy(() -> client.analisar(prompt())).isInstanceOf(GeminiIndisponivelException.class);
		when(httpClient.send(any(), any())).thenThrow(new IOException("falha externa"));
		assertThatThrownBy(() -> client.analisar(prompt())).isInstanceOf(GeminiIndisponivelException.class);
	}

	@SuppressWarnings("unchecked")
	private void responder(int status, String body) throws Exception {
		HttpResponse<String> response = mock(HttpResponse.class);
		when(response.statusCode()).thenReturn(status);
		when(response.body()).thenReturn(body);
		doReturn(response).when(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
	}

	private GeminiPrompt prompt() {
		return new GeminiPrompt("Ideia", "Problema", "Solução", "Benefícios", "Categoria", "Estratégia", "Categoria");
	}

	private String respostaValida() {
		return """
				{"status":"completed","model":"gemini-3.5-flash-lite","steps":[{"type":"model_output","content":[
				{"type":"text","text":"{\\"pontuacaoGeral\\":87,\\"prioridadeSugerida\\":5,\\"resumoExecutivo\\":\\"Boa ideia\\",\\"pontosFortes\\":[\\"Impacto\\"],\\"riscos\\":[\\"Adoção\\"],\\"recomendacoes\\":[\\"Piloto\\"]}"}
				]}]}
				""";
	}
}
