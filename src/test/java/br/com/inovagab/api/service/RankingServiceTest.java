package br.com.inovagab.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;

import br.com.inovagab.api.dto.MedalhaRanking;
import br.com.inovagab.api.dto.RankingColaboradorResponse;
import br.com.inovagab.api.model.Ideia;
import br.com.inovagab.api.model.Role;
import br.com.inovagab.api.model.Usuario;
import br.com.inovagab.api.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class RankingServiceTest {

	@Mock private MongoTemplate mongoTemplate;
	@Mock private UsuarioRepository usuarioRepository;
	@Mock private UsuarioAutenticadoService usuarioAutenticadoService;
	private RankingService rankingService;

	@BeforeEach
	void configurar() {
		rankingService = new RankingService(mongoTemplate, usuarioRepository, usuarioAutenticadoService);
		when(usuarioAutenticadoService.buscarAtivo("usuario-id")).thenReturn(new Usuario());
	}

	@Test
	void rankingVazioQuandoNaoHaProjetosConcluidos() {
		agregacaoRetorna(List.of());

		assertThat(rankingService.colaboradores("usuario-id")).isEmpty();
		verify(usuarioRepository, never()).findAllById(any());
	}

	@Test
	void pipelineExigeIdeiaAprovadaEProjetoConcluido() {
		agregacaoRetorna(List.of());

		rankingService.colaboradores("usuario-id");

		List<Document> pipeline = capturarPipeline();
		assertThat(pipeline).hasSize(5);
		assertThat(pipeline.get(0).get("$match", Document.class)).containsEntry("status", "APROVADA");
		Document lookup = pipeline.get(2).get("$lookup", Document.class);
		assertThat(lookup).containsEntry("from", "projetos")
				.containsEntry("localField", "ideiaIdTexto")
				.containsEntry("foreignField", "ideiaOrigemId");
		assertThat(lookup.getList("pipeline", Document.class)).singleElement().satisfies(stage ->
				assertThat(stage.get("$match", Document.class)).containsEntry("status", "CONCLUIDO"));
		assertThat(pipeline.get(4).get("$match", Document.class).get("ideiasImplementadas", Document.class))
				.containsEntry("$gt", 0);
	}

	@Test
	void pipelineContaCadaIdeiaImplementadaSomenteUmaVez() {
		agregacaoRetorna(List.of());

		rankingService.colaboradores("usuario-id");

		List<Document> pipeline = capturarPipeline();
		assertThat(pipeline).noneMatch(stage -> stage.containsKey("$unwind"));
		Document group = pipeline.get(3).get("$group", Document.class);
		assertThat(group).containsEntry("_id", "$autorId");
		assertThat(group.get("ideiasAprovadas", Document.class)).containsEntry("$sum", 1);
		List<Object> cond = group.get("ideiasImplementadas", Document.class).get("$sum", Document.class)
				.getList("$cond", Object.class);
		assertThat(cond).containsExactly(
				new Document("$gt", List.of(new Document("$size", "$projetosConcluidos"), 0)), 1, 0);
	}

	@Test
	void operadorAtivoComIdeiaAprovadaEProjetoConcluidoEntraNoRanking() {
		agregacaoRetorna(List.of(pontuacao("autor-1", 2, 1)));
		when(usuarioRepository.findAllById(any())).thenReturn(List.of(usuario("autor-1", "Ana", "Empresa A")));

		assertThat(rankingService.colaboradores("usuario-id")).singleElement().satisfies(item -> {
			assertThat(item.posicao()).isEqualTo(1);
			assertThat(item.nome()).isEqualTo("Ana");
			assertThat(item.empresa()).isEqualTo("Empresa A");
			assertThat(item.ideiasAprovadas()).isEqualTo(2);
			assertThat(item.ideiasImplementadas()).isEqualTo(1);
			assertThat(item.medalha()).isEqualTo(MedalhaRanking.OURO);
		});
	}

	@Test
	void operadorInativoNaoEntraNoRanking() {
		agregacaoRetorna(List.of(pontuacao("autor-1", 2, 1)));
		Usuario usuario = usuario("autor-1", "Ana", "Empresa A");
		usuario.setAtivo(false);
		when(usuarioRepository.findAllById(any())).thenReturn(List.of(usuario));

		assertThat(rankingService.colaboradores("usuario-id")).isEmpty();
	}

	@ParameterizedTest
	@EnumSource(value = Role.class, names = { "GESTOR", "LIDERANCA" })
	void gestorELiderancaNaoEntramNoRanking(Role role) {
		agregacaoRetorna(List.of(pontuacao("autor-1", 2, 1)));
		Usuario usuario = usuario("autor-1", "Ana", "Empresa A");
		usuario.setRole(role);
		when(usuarioRepository.findAllById(any())).thenReturn(List.of(usuario));

		assertThat(rankingService.colaboradores("usuario-id")).isEmpty();
	}

	@Test
	void autorInexistenteEIgnorado() {
		agregacaoRetorna(List.of(pontuacao("autor-inexistente", 2, 1)));
		when(usuarioRepository.findAllById(any())).thenReturn(List.of());

		assertThat(rankingService.colaboradores("usuario-id")).isEmpty();
	}

	@Test
	void ordenaPorImplementadasAprovadasENomeEAtribuiMedalhas() {
		agregacaoRetorna(List.of(
				pontuacao("autor-1", 3, 3),
				pontuacao("autor-2", 10, 2),
				pontuacao("autor-3", 5, 2),
				pontuacao("autor-4", 5, 2)));
		when(usuarioRepository.findAllById(any())).thenReturn(List.of(
				usuario("autor-3", "Carlos", "Empresa C"),
				usuario("autor-1", "Ana", "Empresa A"),
				usuario("autor-4", "Bruno", "Empresa B"),
				usuario("autor-2", "Daniela", "Empresa D")));

		List<RankingColaboradorResponse> ranking = rankingService.colaboradores("usuario-id");

		assertThat(ranking).extracting(RankingColaboradorResponse::nome)
				.containsExactly("Ana", "Daniela", "Bruno", "Carlos");
		assertThat(ranking).extracting(RankingColaboradorResponse::posicao).containsExactly(1, 2, 3, 4);
		assertThat(ranking).extracting(RankingColaboradorResponse::medalha).containsExactly(
				MedalhaRanking.OURO, MedalhaRanking.PRATA, MedalhaRanking.BRONZE, MedalhaRanking.SEM_MEDALHA);
		verify(usuarioRepository).findAllById(any());
	}

	private void agregacaoRetorna(List<Document> documentos) {
		when(mongoTemplate.aggregate(any(Aggregation.class), eq(Ideia.class), eq(Document.class)))
				.thenReturn(new AggregationResults<>(documentos, new Document()));
	}

	private List<Document> capturarPipeline() {
		ArgumentCaptor<Aggregation> captor = ArgumentCaptor.forClass(Aggregation.class);
		verify(mongoTemplate).aggregate(captor.capture(), eq(Ideia.class), eq(Document.class));
		return captor.getValue().toPipeline(Aggregation.DEFAULT_CONTEXT);
	}

	private Document pontuacao(String autorId, int aprovadas, int implementadas) {
		return new Document("_id", autorId).append("ideiasAprovadas", aprovadas)
				.append("ideiasImplementadas", implementadas);
	}

	private Usuario usuario(String id, String nome, String empresa) {
		Usuario usuario = new Usuario();
		usuario.setId(id);
		usuario.setNome(nome);
		usuario.setEmpresa(empresa);
		usuario.setRole(Role.OPERADOR);
		usuario.setAtivo(true);
		return usuario;
	}
}
