package br.com.inovagab.api.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "estrategias")
public class Estrategia {

	@Id
	private String id;
	private String titulo;
	private String descricao;

	@Indexed
	private LocalDate data;

	@Indexed
	private String categoria;

	@Indexed
	private String campanha;

	@Indexed
	private StatusEstrategia status = StatusEstrategia.RASCUNHO;

	private String criadoPorId;
	private String atualizadoPorId;

	@CreatedDate
	private Instant criadoEm;

	@LastModifiedDate
	private Instant atualizadoEm;

	@Version
	private Long versao;

	private List<HistoricoEstrategia> historico = new ArrayList<>();

	public Estrategia() {
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTitulo() {
		return titulo;
	}

	public void setTitulo(String titulo) {
		this.titulo = titulo;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}

	public LocalDate getData() {
		return data;
	}

	public void setData(LocalDate data) {
		this.data = data;
	}

	public String getCategoria() {
		return categoria;
	}

	public void setCategoria(String categoria) {
		this.categoria = categoria;
	}

	public String getCampanha() {
		return campanha;
	}

	public void setCampanha(String campanha) {
		this.campanha = campanha;
	}

	public StatusEstrategia getStatus() {
		return status;
	}

	public void setStatus(StatusEstrategia status) {
		this.status = status;
	}

	public String getCriadoPorId() {
		return criadoPorId;
	}

	public void setCriadoPorId(String criadoPorId) {
		this.criadoPorId = criadoPorId;
	}

	public String getAtualizadoPorId() {
		return atualizadoPorId;
	}

	public void setAtualizadoPorId(String atualizadoPorId) {
		this.atualizadoPorId = atualizadoPorId;
	}

	public Instant getCriadoEm() {
		return criadoEm;
	}

	public void setCriadoEm(Instant criadoEm) {
		this.criadoEm = criadoEm;
	}

	public Instant getAtualizadoEm() {
		return atualizadoEm;
	}

	public void setAtualizadoEm(Instant atualizadoEm) {
		this.atualizadoEm = atualizadoEm;
	}

	public Long getVersao() {
		return versao;
	}

	public void setVersao(Long versao) {
		this.versao = versao;
	}

	public List<HistoricoEstrategia> getHistorico() {
		return historico;
	}

	public void setHistorico(List<HistoricoEstrategia> historico) {
		this.historico = historico == null ? new ArrayList<>() : historico;
	}

	public void adicionarHistorico(HistoricoEstrategia item) {
		historico.add(item);
	}
}
