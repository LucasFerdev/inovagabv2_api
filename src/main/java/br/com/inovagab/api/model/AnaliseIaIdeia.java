package br.com.inovagab.api.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class AnaliseIaIdeia {

	public static final String AVISO_PADRAO = "Análise gerada por inteligência artificial. A decisão final pertence ao gestor.";

	private String ideiaId;
	private Integer pontuacaoGeral;
	private Integer prioridadeSugerida;
	private String resumoExecutivo;
	private List<String> pontosFortes = new ArrayList<>();
	private List<String> riscos = new ArrayList<>();
	private List<String> recomendacoes = new ArrayList<>();
	private String modelo;
	private Instant geradoEm;
	private String aviso = AVISO_PADRAO;

	public AnaliseIaIdeia() {
	}

	public AnaliseIaIdeia(String ideiaId, Integer pontuacaoGeral, Integer prioridadeSugerida, String resumoExecutivo,
			List<String> pontosFortes, List<String> riscos, List<String> recomendacoes, String modelo, Instant geradoEm,
			String aviso) {
		this.ideiaId = ideiaId;
		this.pontuacaoGeral = pontuacaoGeral;
		this.prioridadeSugerida = prioridadeSugerida;
		this.resumoExecutivo = resumoExecutivo;
		this.pontosFortes = copiar(pontosFortes);
		this.riscos = copiar(riscos);
		this.recomendacoes = copiar(recomendacoes);
		this.modelo = modelo;
		this.geradoEm = geradoEm;
		this.aviso = aviso;
	}

	private List<String> copiar(List<String> valores) {
		return valores == null ? new ArrayList<>() : new ArrayList<>(valores);
	}

	public String getIdeiaId() { return ideiaId; }
	public void setIdeiaId(String ideiaId) { this.ideiaId = ideiaId; }
	public Integer getPontuacaoGeral() { return pontuacaoGeral; }
	public void setPontuacaoGeral(Integer pontuacaoGeral) { this.pontuacaoGeral = pontuacaoGeral; }
	public Integer getPrioridadeSugerida() { return prioridadeSugerida; }
	public void setPrioridadeSugerida(Integer prioridadeSugerida) { this.prioridadeSugerida = prioridadeSugerida; }
	public String getResumoExecutivo() { return resumoExecutivo; }
	public void setResumoExecutivo(String resumoExecutivo) { this.resumoExecutivo = resumoExecutivo; }
	public List<String> getPontosFortes() { return pontosFortes; }
	public void setPontosFortes(List<String> pontosFortes) { this.pontosFortes = copiar(pontosFortes); }
	public List<String> getRiscos() { return riscos; }
	public void setRiscos(List<String> riscos) { this.riscos = copiar(riscos); }
	public List<String> getRecomendacoes() { return recomendacoes; }
	public void setRecomendacoes(List<String> recomendacoes) { this.recomendacoes = copiar(recomendacoes); }
	public String getModelo() { return modelo; }
	public void setModelo(String modelo) { this.modelo = modelo; }
	public Instant getGeradoEm() { return geradoEm; }
	public void setGeradoEm(Instant geradoEm) { this.geradoEm = geradoEm; }
	public String getAviso() { return aviso; }
	public void setAviso(String aviso) { this.aviso = aviso; }
}
