package br.com.inovagab.api.model;

import java.time.Instant;
import java.time.LocalDate;

public class HistoricoEstrategia {

	private String id;
	private AcaoHistoricoEstrategia acao;
	private Instant dataHora;
	private String usuarioId;
	private String titulo;
	private String descricao;
	private LocalDate data;
	private String categoria;
	private String campanha;
	private StatusEstrategia status;

	public HistoricoEstrategia() {
	}

	public HistoricoEstrategia(String id, AcaoHistoricoEstrategia acao, Instant dataHora, String usuarioId,
			String titulo, String descricao, LocalDate data, String categoria, String campanha, StatusEstrategia status) {
		this.id = id;
		this.acao = acao;
		this.dataHora = dataHora;
		this.usuarioId = usuarioId;
		this.titulo = titulo;
		this.descricao = descricao;
		this.data = data;
		this.categoria = categoria;
		this.campanha = campanha;
		this.status = status;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public AcaoHistoricoEstrategia getAcao() {
		return acao;
	}

	public void setAcao(AcaoHistoricoEstrategia acao) {
		this.acao = acao;
	}

	public Instant getDataHora() {
		return dataHora;
	}

	public void setDataHora(Instant dataHora) {
		this.dataHora = dataHora;
	}

	public String getUsuarioId() {
		return usuarioId;
	}

	public void setUsuarioId(String usuarioId) {
		this.usuarioId = usuarioId;
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
}
