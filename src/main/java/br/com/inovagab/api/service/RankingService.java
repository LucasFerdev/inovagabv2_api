package br.com.inovagab.api.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.stereotype.Service;

import br.com.inovagab.api.dto.MedalhaRanking;
import br.com.inovagab.api.dto.RankingColaboradorResponse;
import br.com.inovagab.api.model.Ideia;
import br.com.inovagab.api.model.Role;
import br.com.inovagab.api.model.StatusIdeia;
import br.com.inovagab.api.model.StatusProjeto;
import br.com.inovagab.api.model.Usuario;
import br.com.inovagab.api.repository.UsuarioRepository;

@Service
public class RankingService {

	private static final String COLECAO_PROJETOS = "projetos";
	private final MongoTemplate mongoTemplate;
	private final UsuarioRepository usuarioRepository;
	private final UsuarioAutenticadoService usuarioAutenticadoService;

	public RankingService(MongoTemplate mongoTemplate, UsuarioRepository usuarioRepository,
			UsuarioAutenticadoService usuarioAutenticadoService) {
		this.mongoTemplate = mongoTemplate;
		this.usuarioRepository = usuarioRepository;
		this.usuarioAutenticadoService = usuarioAutenticadoService;
	}

	public List<RankingColaboradorResponse> colaboradores(String usuarioId) {
		usuarioAutenticadoService.buscarAtivo(usuarioId);
		List<Document> pontuacoes = mongoTemplate.aggregate(agregacao(), Ideia.class, Document.class).getMappedResults();
		if (pontuacoes.isEmpty()) return List.of();

		Map<String, Document> pontuacoesPorAutor = new HashMap<>();
		for (Document pontuacao : pontuacoes) {
			String autorId = pontuacao.getString("_id");
			if (autorId != null && !autorId.isBlank()) pontuacoesPorAutor.put(autorId, pontuacao);
		}
		if (pontuacoesPorAutor.isEmpty()) return List.of();

		List<Participante> participantes = new ArrayList<>();
		for (Usuario usuario : usuarioRepository.findAllById(pontuacoesPorAutor.keySet())) {
			Document pontuacao = pontuacoesPorAutor.get(usuario.getId());
			if (pontuacao != null && usuario.isAtivo() && usuario.getRole() == Role.OPERADOR) {
				participantes.add(new Participante(usuario.getNome(), usuario.getEmpresa(),
						numero(pontuacao, "ideiasAprovadas"), numero(pontuacao, "ideiasImplementadas")));
			}
		}

		participantes.sort(Comparator.comparingLong(Participante::ideiasImplementadas).reversed()
				.thenComparing(Comparator.comparingLong(Participante::ideiasAprovadas).reversed())
				.thenComparing(Participante::nome));

		List<RankingColaboradorResponse> ranking = new ArrayList<>(participantes.size());
		for (int indice = 0; indice < participantes.size(); indice++) {
			Participante participante = participantes.get(indice);
			int posicao = indice + 1;
			ranking.add(new RankingColaboradorResponse(posicao, participante.nome(), participante.empresa(),
					participante.ideiasAprovadas(), participante.ideiasImplementadas(), medalha(posicao)));
		}
		return List.copyOf(ranking);
	}

	private Aggregation agregacao() {
		Document lookup = new Document("from", COLECAO_PROJETOS)
				.append("localField", "ideiaIdTexto")
				.append("foreignField", "ideiaOrigemId")
				.append("pipeline", List.of(new Document("$match",
						new Document("status", StatusProjeto.CONCLUIDO.name()))))
				.append("as", "projetosConcluidos");
		Document implementada = new Document("$gt", List.of(
				new Document("$size", "$projetosConcluidos"), 0));
		Document group = new Document("_id", "$autorId")
				.append("ideiasAprovadas", new Document("$sum", 1))
				.append("ideiasImplementadas", new Document("$sum",
						new Document("$cond", List.of(implementada, 1, 0))));
		return Aggregation.newAggregation(
				Aggregation.stage(new Document("$match",
						new Document("status", StatusIdeia.APROVADA.name()))),
				Aggregation.stage(new Document("$set",
						new Document("ideiaIdTexto", new Document("$toString", "$_id")))),
				Aggregation.stage(new Document("$lookup", lookup)),
				Aggregation.stage(new Document("$group", group)),
				Aggregation.stage(new Document("$match",
						new Document("ideiasImplementadas", new Document("$gt", 0)))));
	}

	private long numero(Document documento, String campo) {
		Object valor = documento.get(campo);
		return valor instanceof Number numero ? numero.longValue() : 0L;
	}

	private MedalhaRanking medalha(int posicao) {
		return switch (posicao) {
			case 1 -> MedalhaRanking.OURO;
			case 2 -> MedalhaRanking.PRATA;
			case 3 -> MedalhaRanking.BRONZE;
			default -> MedalhaRanking.SEM_MEDALHA;
		};
	}

	private record Participante(String nome, String empresa, long ideiasAprovadas, long ideiasImplementadas) {
	}
}
