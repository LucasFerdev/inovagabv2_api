# InovaGAB API

Backend do InovaGAB para organizar estratégias de inovação, receber e avaliar ideias, acompanhar projetos,
consolidar indicadores executivos e oferecer análise consultiva de ideias com inteligência artificial.

## Arquitetura e tecnologias

- Java 21 e Maven Wrapper.
- Spring Boot 4.1.1 e Spring MVC.
- Spring Security com autenticação JWT HS256 e sessão stateless.
- Spring Data MongoDB com MongoDB Atlas, auditoria, índices e controle otimista por versão.
- Gemini Interactions API para análise consultiva de ideias.
- OpenAPI 3 e Swagger UI com springdoc-openapi 3.x.
- DTOs em todas as entradas e respostas; documentos MongoDB não são expostos diretamente.

Os relacionamentos entre documentos usam somente IDs `String`. O projeto não utiliza JPA, Hibernate, banco
relacional, Lombok ou `@DBRef`.

## Pré-requisitos

- JDK 21 configurado no IntelliJ IDEA e em `JAVA_HOME`.
- Acesso a um cluster MongoDB Atlas.
- Chave da Gemini API para utilizar a análise por IA.
- PowerShell no Windows para executar os exemplos abaixo.

## Variáveis de ambiente

Configure as variáveis no IntelliJ em **Run/Debug Configurations > Environment variables** ou diretamente no
terminal. Nunca grave valores reais em `application.properties`, `.env.example` ou arquivos versionados.

| Variável | Obrigatória | Descrição |
|---|---:|---|
| `MONGODB_URI` | Sim | URI completa e confidencial do MongoDB Atlas |
| `JWT_SECRET` | Sim | Chave Base64 forte, com pelo menos 256 bits |
| `JWT_EXPIRATION_SECONDS` | Não | Validade do JWT; padrão `3600` |
| `GEMINI_API_KEY` | Para IA | Chave confidencial da Gemini API |
| `GEMINI_MODEL` | Não | Modelo; padrão `gemini-3.5-flash-lite` |
| `GEMINI_BASE_URL` | Não | Base oficial; padrão `https://generativelanguage.googleapis.com` |
| `GEMINI_TIMEOUT_SECONDS` | Não | Timeout da chamada; padrão `20` segundos |

O arquivo `.env.example` contém apenas marcadores fictícios. `.env` e `.env.*` são ignorados pelo Git, com exceção
de `.env.example`.

## MongoDB Atlas

1. Crie o banco `inovagab` no cluster.
2. Crie um usuário com as permissões mínimas necessárias para leitura, escrita e criação dos índices usados pela API.
3. Autorize somente os endereços de rede necessários no Atlas.
4. Defina a URI completa exclusivamente em `MONGODB_URI`.
5. Não inclua a URI em logs, capturas de tela, commits ou arquivos de configuração.

Na inicialização, a aplicação garante os índices de usuários, estratégias, ideias e projetos. As coleções principais
são `usuarios`, `estrategias`, `ideias` e `projetos`.

## Configuração segura do Gemini

A aplicação utiliza `POST /v1beta/interactions` no host configurado por `GEMINI_BASE_URL`. A chave é enviada somente
no header `x-goog-api-key`, nunca na URL, no documento MongoDB, em respostas ou logs.

O provedor recebe apenas título, problema, solução proposta, benefícios esperados e categoria da ideia, além do
título e categoria da estratégia. Nome, e-mail, empresa, IDs internos, JWT e outros dados pessoais não são enviados.

A resposta é solicitada como `application/json` com JSON Schema e validada novamente pela aplicação antes da
persistência. A IA não aprova, rejeita, prioriza nem altera automaticamente o status da ideia.

## Execução

### IntelliJ IDEA

1. Importe o projeto Maven e selecione o JDK 21.
2. Configure as variáveis de ambiente na execução de `InovagabApiApplication`.
3. Execute `br.com.inovagab.api.InovagabApiApplication`.

### PowerShell

Com as variáveis configuradas no ambiente atual:

```powershell
.\mvnw.cmd spring-boot:run
```

Para compilar e executar todos os testes, que não acessam o MongoDB Atlas nem a Gemini API real:

```powershell
.\mvnw.cmd clean test
```

## Perfis e permissões

| Recurso | OPERADOR | GESTOR | LIDERANCA |
|---|---|---|---|
| Cadastro e login | Público | Público | Público |
| Estratégias | Consulta | Consulta | Gestão completa |
| Ideias | Cria e administra próprias ideias enviadas | Consulta e avaliação | Consulta |
| Projetos | Sem acesso | Gestão completa | Consulta |
| Dashboard | Sem acesso | Sem acesso | Consulta |
| Executar análise por IA | Não | Sim | Não |
| Consultar análise por IA | Não | Sim | Sim |

## Endpoints

### Autenticação

- `POST /api/auth/cadastro`
- `POST /api/auth/login`
- `GET /api/auth/me`

### Estratégias

- `POST /api/estrategias`
- `GET /api/estrategias`
- `GET /api/estrategias/ativas`
- `GET /api/estrategias/{id}`
- `GET /api/estrategias/{id}/historico`
- `PUT /api/estrategias/{id}`
- `PATCH /api/estrategias/{id}/ativar`
- `PATCH /api/estrategias/{id}/desativar`
- `DELETE /api/estrategias/{id}`

### Ideias

- `POST /api/ideias`
- `GET /api/ideias/minhas`
- `GET /api/ideias`
- `GET /api/ideias/{id}`
- `GET /api/ideias/{id}/historico`
- `PUT /api/ideias/{id}`
- `DELETE /api/ideias/{id}`
- `PATCH /api/ideias/{id}/analisar`
- `PATCH /api/ideias/{id}/priorizar`
- `PATCH /api/ideias/{id}/aprovar`
- `PATCH /api/ideias/{id}/rejeitar`

### Projetos

- `POST /api/projetos`
- `GET /api/projetos`
- `GET /api/projetos/{id}`
- `GET /api/projetos/{id}/historico`
- `PUT /api/projetos/{id}`
- `PATCH /api/projetos/{id}/progresso`
- `PATCH /api/projetos/{id}/resultados`
- `PATCH /api/projetos/{id}/concluir`
- `DELETE /api/projetos/{id}`

### Dashboard e IA

- `GET /api/dashboard/resumo`
- `GET /api/dashboard/estrategias/{estrategiaId}`
- `GET /api/dashboard/projetos/{projetoId}`
- `POST /api/ia/ideias/{ideiaId}/analisar?recalcular=false`
- `GET /api/ia/ideias/{ideiaId}/analise`

### Operação e documentação

- `GET /actuator/health`
- `GET /actuator/info`
- `GET /v3/api-docs`
- `GET /swagger-ui.html`

## Autenticação

Cadastro público sempre cria um usuário `OPERADOR`:

```http
POST /api/auth/cadastro
Content-Type: application/json

{
  "nome": "Nome do usuário",
  "email": "usuario@example.com",
  "senha": "senha-segura",
  "empresa": "Empresa exemplo"
}
```

Após o login, envie o JWT nas rotas protegidas:

```http
Authorization: Bearer <TOKEN_JWT>
```

Nunca compartilhe ou registre o token emitido.

## Análise por IA

A ideia deve existir e estar em `EM_ANALISE`. A primeira chamada gera e armazena a análise. Chamadas posteriores
retornam a versão persistida sem consumir nova cota, exceto quando o Gestor usa `recalcular=true`.

```http
POST /api/ia/ideias/<IDEIA_ID>/analisar?recalcular=false
Authorization: Bearer <TOKEN_GESTOR>
```

Exemplo de resposta:

```json
{
  "ideiaId": "<IDEIA_ID>",
  "pontuacaoGeral": 85,
  "prioridadeSugerida": 4,
  "resumoExecutivo": "A ideia apresenta impacto relevante e viabilidade moderada.",
  "pontosFortes": ["Alinhamento estratégico", "Potencial de eficiência"],
  "riscos": ["Dependência de adesão operacional"],
  "recomendacoes": ["Executar um piloto controlado"],
  "modelo": "gemini-3.5-flash-lite",
  "geradoEm": "2027-01-10T12:00:00Z",
  "aviso": "Análise gerada por inteligência artificial. A decisão final pertence ao gestor."
}
```

## Códigos HTTP

- `200 OK`: consulta ou alteração concluída.
- `201 Created`: recurso criado.
- `204 No Content`: arquivamento ou cancelamento lógico concluído.
- `400 Bad Request`: JSON ou dados de entrada inválidos.
- `401 Unauthorized`: JWT ausente, inválido ou expirado.
- `403 Forbidden`: perfil sem permissão ou usuário inativo.
- `404 Not Found`: rota ou recurso inexistente.
- `405 Method Not Allowed`: método HTTP não suportado pela rota.
- `409 Conflict`: transição de negócio ou concorrência inválida.
- `429 Too Many Requests`: limite temporário do Gemini atingido.
- `502 Bad Gateway`: resposta inválida ou indisponibilidade do Gemini.
- `503 Service Unavailable`: integração Gemini sem chave configurada.
- `504 Gateway Timeout`: timeout na chamada ao Gemini.
- `500 Internal Server Error`: falha inesperada, sem exposição de detalhes internos.

## OpenAPI e Swagger

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Documento OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Somente os caminhos da documentação são públicos. Os endpoints de negócio continuam protegidos normalmente.

## Emulador Android

No emulador Android, `localhost` aponta para o próprio emulador. Para acessar a API executada na máquina de
desenvolvimento, use `http://10.0.2.2:8080`.

> Nunca versione chaves do Gemini, URIs do MongoDB, senhas, tokens JWT ou quaisquer outras credenciais.
