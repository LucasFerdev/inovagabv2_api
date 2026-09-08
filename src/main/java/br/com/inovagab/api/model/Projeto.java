package br.com.inovagab.api.model;

import java.math.BigDecimal;
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

@Document(collection = "projetos")
public class Projeto {

	@Id private String id;
	private String nome;
	private String descricao;
	@Indexed private String estrategiaId;
	@Indexed private String ideiaOrigemId;
	@Indexed private EtapaProjeto etapa = EtapaProjeto.PLANEJAMENTO;
	@Indexed private StatusProjeto status = StatusProjeto.PLANEJADO;
	private Integer percentualProgresso = 0;
	private BigDecimal investimento;
	@Indexed private LocalDate prazo;
	private BigDecimal retornoFinanceiro = BigDecimal.ZERO;
	private BigDecimal ganhoProdutividadePercentual = BigDecimal.ZERO;
	private String resultado;
	@Indexed private String gestorResponsavelId;
	@CreatedDate @Indexed private Instant criadoEm;
	@LastModifiedDate private Instant atualizadoEm;
	@Version private Long versao;
	private List<HistoricoProjeto> historico = new ArrayList<>();

	public Projeto() {
	}

	public String getId() { return id; }
	public void setId(String id) { this.id = id; }
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
	public String getGestorResponsavelId() { return gestorResponsavelId; }
	public void setGestorResponsavelId(String gestorResponsavelId) { this.gestorResponsavelId = gestorResponsavelId; }
	public Instant getCriadoEm() { return criadoEm; }
	public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
	public Instant getAtualizadoEm() { return atualizadoEm; }
	public void setAtualizadoEm(Instant atualizadoEm) { this.atualizadoEm = atualizadoEm; }
	public Long getVersao() { return versao; }
	public void setVersao(Long versao) { this.versao = versao; }
	public List<HistoricoProjeto> getHistorico() { return historico; }
	public void setHistorico(List<HistoricoProjeto> historico) { this.historico = historico == null ? new ArrayList<>() : historico; }
	public void adicionarHistorico(HistoricoProjeto item) { historico.add(item); }
}
