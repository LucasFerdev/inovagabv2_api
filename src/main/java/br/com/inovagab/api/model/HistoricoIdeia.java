package br.com.inovagab.api.model;

import java.time.Instant;

public class HistoricoIdeia {

	private String id;
	private AcaoHistoricoIdeia acao;
	private Instant dataHora;
	private String usuarioId;
	private String titulo;
	private String problema;
	private String solucaoProposta;
	private String beneficiosEsperados;
	private String categoria;
	private String estrategiaId;
	private StatusIdeia status;
	private Integer prioridade;
	private String justificativaAvaliacao;
	private AnaliseIaIdeia analiseIa;

	public HistoricoIdeia() {
	}

	public HistoricoIdeia(String id, AcaoHistoricoIdeia acao, Instant dataHora, String usuarioId, String titulo,
			String problema, String solucaoProposta, String beneficiosEsperados, String categoria, String estrategiaId,
			StatusIdeia status, Integer prioridade, String justificativaAvaliacao) {
		this.id = id;
		this.acao = acao;
		this.dataHora = dataHora;
		this.usuarioId = usuarioId;
		this.titulo = titulo;
		this.problema = problema;
		this.solucaoProposta = solucaoProposta;
		this.beneficiosEsperados = beneficiosEsperados;
		this.categoria = categoria;
		this.estrategiaId = estrategiaId;
		this.status = status;
		this.prioridade = prioridade;
		this.justificativaAvaliacao = justificativaAvaliacao;
	}

	public HistoricoIdeia(String id, AcaoHistoricoIdeia acao, Instant dataHora, String usuarioId, String titulo,
			String problema, String solucaoProposta, String beneficiosEsperados, String categoria, String estrategiaId,
			StatusIdeia status, Integer prioridade, String justificativaAvaliacao, AnaliseIaIdeia analiseIa) {
		this(id, acao, dataHora, usuarioId, titulo, problema, solucaoProposta, beneficiosEsperados, categoria,
				estrategiaId, status, prioridade, justificativaAvaliacao);
		this.analiseIa = analiseIa;
	}

	public String getId() { return id; }
	public void setId(String id) { this.id = id; }
	public AcaoHistoricoIdeia getAcao() { return acao; }
	public void setAcao(AcaoHistoricoIdeia acao) { this.acao = acao; }
	public Instant getDataHora() { return dataHora; }
	public void setDataHora(Instant dataHora) { this.dataHora = dataHora; }
	public String getUsuarioId() { return usuarioId; }
	public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }
	public String getTitulo() { return titulo; }
	public void setTitulo(String titulo) { this.titulo = titulo; }
	public String getProblema() { return problema; }
	public void setProblema(String problema) { this.problema = problema; }
	public String getSolucaoProposta() { return solucaoProposta; }
	public void setSolucaoProposta(String solucaoProposta) { this.solucaoProposta = solucaoProposta; }
	public String getBeneficiosEsperados() { return beneficiosEsperados; }
	public void setBeneficiosEsperados(String beneficiosEsperados) { this.beneficiosEsperados = beneficiosEsperados; }
	public String getCategoria() { return categoria; }
	public void setCategoria(String categoria) { this.categoria = categoria; }
	public String getEstrategiaId() { return estrategiaId; }
	public void setEstrategiaId(String estrategiaId) { this.estrategiaId = estrategiaId; }
	public StatusIdeia getStatus() { return status; }
	public void setStatus(StatusIdeia status) { this.status = status; }
	public Integer getPrioridade() { return prioridade; }
	public void setPrioridade(Integer prioridade) { this.prioridade = prioridade; }
	public String getJustificativaAvaliacao() { return justificativaAvaliacao; }
	public void setJustificativaAvaliacao(String justificativaAvaliacao) { this.justificativaAvaliacao = justificativaAvaliacao; }
	public AnaliseIaIdeia getAnaliseIa() { return analiseIa; }
	public void setAnaliseIa(AnaliseIaIdeia analiseIa) { this.analiseIa = analiseIa; }
}
