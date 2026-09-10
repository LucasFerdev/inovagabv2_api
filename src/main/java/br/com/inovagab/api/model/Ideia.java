package br.com.inovagab.api.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "ideias")
public class Ideia {

	@Id
	private String id;
	private String titulo;
	private String problema;
	private String solucaoProposta;
	private String beneficiosEsperados;
	@Indexed
	private String categoria;
	@Indexed
	private String estrategiaId;
	@Indexed
	private String autorId;
	@Indexed
	private StatusIdeia status = StatusIdeia.ENVIADA;
	@Indexed
	private Integer prioridade;
	private String justificativaAvaliacao;
	private String avaliadoPorId;
	private Instant avaliadoEm;
	private AnaliseIaIdeia analiseIa;
	@CreatedDate
	@Indexed
	private Instant criadoEm;
	@LastModifiedDate
	private Instant atualizadoEm;
	@Version
	private Long versao;
	private List<HistoricoIdeia> historico = new ArrayList<>();

	public Ideia() {
	}

	public String getId() { return id; }
	public void setId(String id) { this.id = id; }
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
	public String getAutorId() { return autorId; }
	public void setAutorId(String autorId) { this.autorId = autorId; }
	public StatusIdeia getStatus() { return status; }
	public void setStatus(StatusIdeia status) { this.status = status; }
	public Integer getPrioridade() { return prioridade; }
	public void setPrioridade(Integer prioridade) { this.prioridade = prioridade; }
	public String getJustificativaAvaliacao() { return justificativaAvaliacao; }
	public void setJustificativaAvaliacao(String justificativaAvaliacao) { this.justificativaAvaliacao = justificativaAvaliacao; }
	public String getAvaliadoPorId() { return avaliadoPorId; }
	public void setAvaliadoPorId(String avaliadoPorId) { this.avaliadoPorId = avaliadoPorId; }
	public Instant getAvaliadoEm() { return avaliadoEm; }
	public void setAvaliadoEm(Instant avaliadoEm) { this.avaliadoEm = avaliadoEm; }
	public AnaliseIaIdeia getAnaliseIa() { return analiseIa; }
	public void setAnaliseIa(AnaliseIaIdeia analiseIa) { this.analiseIa = analiseIa; }
	public Instant getCriadoEm() { return criadoEm; }
	public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
	public Instant getAtualizadoEm() { return atualizadoEm; }
	public void setAtualizadoEm(Instant atualizadoEm) { this.atualizadoEm = atualizadoEm; }
	public Long getVersao() { return versao; }
	public void setVersao(Long versao) { this.versao = versao; }
	public List<HistoricoIdeia> getHistorico() { return historico; }
	public void setHistorico(List<HistoricoIdeia> historico) { this.historico = historico == null ? new ArrayList<>() : historico; }
	public void adicionarHistorico(HistoricoIdeia item) { historico.add(item); }
}
