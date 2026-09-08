package br.com.inovagab.api.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class HistoricoProjeto {

	private String id;
	private AcaoHistoricoProjeto acao;
	private Instant dataHora;
	private String usuarioId;
	private String nome;
	private String descricao;
	private String estrategiaId;
	private String ideiaOrigemId;
	private EtapaProjeto etapa;
	private StatusProjeto status;
	private Integer percentualProgresso;
	private BigDecimal investimento;
	private LocalDate prazo;
	private BigDecimal retornoFinanceiro;
	private BigDecimal ganhoProdutividadePercentual;
	private String resultado;
	private String justificativa;

	public HistoricoProjeto() {
	}

	public HistoricoProjeto(String id, AcaoHistoricoProjeto acao, Instant dataHora, String usuarioId, String nome,
			String descricao, String estrategiaId, String ideiaOrigemId, EtapaProjeto etapa, StatusProjeto status,
			Integer percentualProgresso, BigDecimal investimento, LocalDate prazo, BigDecimal retornoFinanceiro,
			BigDecimal ganhoProdutividadePercentual, String resultado, String justificativa) {
		this.id = id;
		this.acao = acao;
		this.dataHora = dataHora;
		this.usuarioId = usuarioId;
		this.nome = nome;
		this.descricao = descricao;
		this.estrategiaId = estrategiaId;
		this.ideiaOrigemId = ideiaOrigemId;
		this.etapa = etapa;
		this.status = status;
		this.percentualProgresso = percentualProgresso;
		this.investimento = investimento;
		this.prazo = prazo;
		this.retornoFinanceiro = retornoFinanceiro;
		this.ganhoProdutividadePercentual = ganhoProdutividadePercentual;
		this.resultado = resultado;
		this.justificativa = justificativa;
	}

	public String getId() { return id; }
	public void setId(String id) { this.id = id; }
	public AcaoHistoricoProjeto getAcao() { return acao; }
	public void setAcao(AcaoHistoricoProjeto acao) { this.acao = acao; }
	public Instant getDataHora() { return dataHora; }
	public void setDataHora(Instant dataHora) { this.dataHora = dataHora; }
	public String getUsuarioId() { return usuarioId; }
	public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }
	public String getNome() { return nome; }
	public void setNome(String nome) { this.nome = nome; }
	public String getDescricao() { return descricao; }
	public void setDescricao(String descricao) { this.descricao = descricao; }
	public String getEstrategiaId() { return estrategiaId; }
	public void setEstrategiaId(String estrategiaId) { this.estrategiaId = estrategiaId; }
	public String getIdeiaOrigemId() { return ideiaOrigemId; }
	public void setIdeiaOrigemId(String ideiaOrigemId) { this.ideiaOrigemId = ideiaOrigemId; }
	public EtapaProjeto getEtapa() { return etapa; }
	public void setEtapa(EtapaProjeto etapa) { this.etapa = etapa; }
	public StatusProjeto getStatus() { return status; }
	public void setStatus(StatusProjeto status) { this.status = status; }
	public Integer getPercentualProgresso() { return percentualProgresso; }
	public void setPercentualProgresso(Integer percentualProgresso) { this.percentualProgresso = percentualProgresso; }
	public BigDecimal getInvestimento() { return investimento; }
	public void setInvestimento(BigDecimal investimento) { this.investimento = investimento; }
	public LocalDate getPrazo() { return prazo; }
	public void setPrazo(LocalDate prazo) { this.prazo = prazo; }
	public BigDecimal getRetornoFinanceiro() { return retornoFinanceiro; }
	public void setRetornoFinanceiro(BigDecimal retornoFinanceiro) { this.retornoFinanceiro = retornoFinanceiro; }
	public BigDecimal getGanhoProdutividadePercentual() { return ganhoProdutividadePercentual; }
	public void setGanhoProdutividadePercentual(BigDecimal valor) { this.ganhoProdutividadePercentual = valor; }
	public String getResultado() { return resultado; }
	public void setResultado(String resultado) { this.resultado = resultado; }
	public String getJustificativa() { return justificativa; }
	public void setJustificativa(String justificativa) { this.justificativa = justificativa; }
}
