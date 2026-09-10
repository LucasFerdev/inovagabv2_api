package br.com.inovagab.api.ia;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import br.com.inovagab.api.config.GeminiProperties;
import br.com.inovagab.api.exception.GeminiIndisponivelException;
import br.com.inovagab.api.exception.GeminiLimiteExcedidoException;
import br.com.inovagab.api.exception.GeminiNaoConfiguradoException;
import br.com.inovagab.api.exception.GeminiRespostaInvalidaException;
import br.com.inovagab.api.exception.GeminiTimeoutException;
import tools.jackson.databind.ObjectMapper;



@Component
public class GeminiRestClient implements GeminiClient {

	private static final String INSTRUCAO_SISTEMA = """
			Responda em português do Brasil. Avalie a ideia de inovação de forma consultiva considerando impacto,
			viabilidade, alinhamento estratégico, inovação e clareza. Não invente informações ausentes.
			A análise deve deixar claro que a decisão final pertence a uma pessoa gestora.
			""";

	private final GeminiProperties properties;
	private final ObjectMapper objectMapper;
	private final HttpClient httpClient;

	@Autowired
	public GeminiRestClient(GeminiProperties properties, ObjectMapper objectMapper) {
		this(properties, objectMapper, HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(properties.timeoutSeconds()))
				.build());
	}

	GeminiRestClient(GeminiProperties properties, ObjectMapper objectMapper, HttpClient httpClient) {
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.httpClient = httpClient;
	}

	@Override
	public GeminiResultado analisar(GeminiPrompt prompt) {
		if (!StringUtils.hasText(properties.apiKey())) {
			throw new GeminiNaoConfiguradoException();
		}
		HttpRequest request = HttpRequest.newBuilder(endpoint())
				.timeout(Duration.ofSeconds(properties.timeoutSeconds()))
				.header("Content-Type", "application/json")
				.header("x-goog-api-key", properties.apiKey())
				.POST(HttpRequest.BodyPublishers.ofString(serializar(criarRequisicao(prompt))))
				.build();
		HttpResponse<String> response;
		try {
			response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		}
		catch (HttpTimeoutException exception) {
			throw new GeminiTimeoutException();
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new GeminiIndisponivelException();
		}
		catch (IOException exception) {
			throw new GeminiIndisponivelException();
		}
		if (response.statusCode() == 429) {
			throw new GeminiLimiteExcedidoException();
		}
		if (response.statusCode() < 200 || response.statusCode() >= 300) {
			throw new GeminiIndisponivelException();
		}
		return extrairResultado(response.body());
	}

	private URI endpoint() {
		String baseUrl = properties.baseUrl().toString().replaceAll("/+$", "");
		return URI.create(baseUrl + "/v1beta/interactions");
	}

	private Map<String, Object> criarRequisicao(GeminiPrompt prompt) {
		Map<String, Object> dados = Map.of(
				"titulo", prompt.titulo(),
				"problema", prompt.problema(),
				"solucaoProposta", prompt.solucaoProposta(),
				"beneficiosEsperados", prompt.beneficiosEsperados(),
				"categoria", prompt.categoria(),
				"tituloEstrategia", prompt.tituloEstrategia(),
				"categoriaEstrategia", prompt.categoriaEstrategia());
		return Map.of(
				"model", properties.model(),
				"input", serializar(dados),
				"system_instruction", INSTRUCAO_SISTEMA,
				"response_format", formatoResposta(),
				"store", false);
	}

	private Map<String, Object> formatoResposta() {
		Map<String, Object> texto = Map.of("type", "string", "minLength", 1);
		Map<String, Object> lista = Map.of("type", "array", "items", texto, "maxItems", 10);
		Map<String, Object> schema = Map.of(
				"type", "object",
				"additionalProperties", false,
				"properties", Map.of(
						"pontuacaoGeral", Map.of("type", "integer", "minimum", 0, "maximum", 100),
						"prioridadeSugerida", Map.of("type", "integer", "minimum", 1, "maximum", 5),
						"resumoExecutivo", texto,
						"pontosFortes", lista,
						"riscos", lista,
						"recomendacoes", lista),
				"required", List.of("pontuacaoGeral", "prioridadeSugerida", "resumoExecutivo", "pontosFortes",
						"riscos", "recomendacoes"));
		return Map.of("type", "text", "mime_type", "application/json", "schema", schema);
	}

	private String serializar(Object valor) {
		try {
			return objectMapper.writeValueAsString(valor);
		}
		catch (RuntimeException exception) {
			throw new GeminiRespostaInvalidaException();
		}
	}

	@SuppressWarnings("unchecked")
	private GeminiResultado extrairResultado(String corpo) {
		try {
			Map<String, Object> resposta = objectMapper.readValue(corpo, Map.class);
			if (!"completed".equals(resposta.get("status"))) {
				throw new GeminiRespostaInvalidaException();
			}
			String texto = extrairTexto((List<Map<String, Object>>) resposta.get("steps"));
			Map<String, Object> analise = objectMapper.readValue(texto, Map.class);
			return new GeminiResultado(inteiro(analise.get("pontuacaoGeral")),
					inteiro(analise.get("prioridadeSugerida")), texto(analise.get("resumoExecutivo")),
					lista(analise.get("pontosFortes")), lista(analise.get("riscos")),
					lista(analise.get("recomendacoes")), texto(resposta.get("model")));
		}
		catch (GeminiRespostaInvalidaException exception) {
			throw exception;
		}
		catch (RuntimeException exception) {
			throw new GeminiRespostaInvalidaException();
		}
	}

	@SuppressWarnings("unchecked")
	private String extrairTexto(List<Map<String, Object>> passos) {
		if (passos == null) throw new GeminiRespostaInvalidaException();
		for (Map<String, Object> passo : passos) {
			if (!"model_output".equals(passo.get("type"))) continue;
			Object conteudo = passo.get("content");
			if (!(conteudo instanceof List<?> itens)) continue;
			for (Object item : itens) {
				if (item instanceof Map<?, ?> mapa && "text".equals(mapa.get("type"))) {
					return texto(mapa.get("text"));
				}
			}
		}
		throw new GeminiRespostaInvalidaException();
	}

	private Integer inteiro(Object valor) {
		return valor instanceof Number numero ? numero.intValue() : null;
	}

	private String texto(Object valor) {
		return valor instanceof String texto ? texto : null;
	}

	private List<String> lista(Object valor) {
		if (!(valor instanceof List<?> itens)) return null;
		return itens.stream().map(this::texto).toList();
	}
}
