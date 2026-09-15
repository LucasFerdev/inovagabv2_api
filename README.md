# InovaGAB API — Gestão de Inovação

API REST para gestão do ciclo de inovação corporativa — estratégias, ideias, projetos, indicadores executivos, ranking de colaboradores e análise consultiva com inteligência artificial.

Backend desenvolvido com Java, Spring Boot, autenticação JWT, MongoDB Atlas e integração com a Gemini API.

Projeto de estudo e portfólio — Desenvolvimento Backend.

## Sumário

- [Sobre o projeto](#sobre-o-projeto)
- [Tecnologias](#tecnologias)
- [Funcionalidades](#funcionalidades)
- [Arquitetura](#arquitetura)
- [Estrutura de pastas](#estrutura-de-pastas)
- [Como executar](#como-executar)
- [Frontend](#frontend)
- [Backend](#backend)
- [Banco de dados](#banco-de-dados)
- [API — Endpoints](#api--endpoints)
- [Insomnia](#insomnia)
- [Boas práticas](#boas-práticas)
- [Autor](#autor)

## Sobre o projeto

O InovaGAB centraliza o fluxo de inovação de uma organização, desde a definição das estratégias até o acompanhamento dos resultados dos projetos:

1. A liderança cria e ativa estratégias de inovação.
2. Operadores enviam ideias alinhadas às estratégias disponíveis.
3. Gestores analisam, priorizam, aprovam ou rejeitam as ideias.
4. Ideias aprovadas podem originar projetos com etapas, progresso, prazos e resultados.
5. A liderança acompanha indicadores consolidados pelo dashboard.
6. Gestores podem solicitar uma análise consultiva da ideia à Gemini API.
7. Todos os perfis acompanham o ranking de colaboradores por ideias implementadas.

A análise gerada por IA tem caráter consultivo: ela não altera o status da ideia nem substitui a decisão do gestor.

## Tecnologias

### Backend

| Categoria | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 4.1.1 |
| API REST | Spring Web MVC |
| Segurança | Spring Security + OAuth2 Resource Server |
| Autenticação | JWT HS256 com sessão stateless |
| Banco de dados | MongoDB Atlas + Spring Data MongoDB |
| Validação | Jakarta Bean Validation |
| Inteligência artificial | Gemini Interactions API |
| Documentação | OpenAPI 3 + Swagger UI |
| Monitoramento | Spring Boot Actuator |
| Build e dependências | Maven Wrapper |
| Testes | JUnit 5, Mockito e Spring Security Test |

## Funcionalidades

### Gestão de inovação

| Funcionalidade | Descrição |
|---|---|
| Autenticação | Cadastro, login e identificação do usuário autenticado com JWT |
| Perfis de acesso | Permissões separadas para `OPERADOR`, `GESTOR` e `LIDERANCA` |
| Estratégias | Criação, consulta, filtros, ativação, desativação, arquivamento e histórico |
| Ideias | Envio, edição, filtros, avaliação, priorização, aprovação, rejeição e histórico |
| Projetos | Planejamento, acompanhamento de etapa e progresso, registro de resultados e conclusão |
| Dashboard | Resumo executivo e indicadores por estratégia e projeto |
| Ranking | Classificação de colaboradores por ideias implementadas |
| Análise por IA | Pontuação, prioridade sugerida, resumo, pontos fortes, riscos e recomendações |
| Paginação | Listagens paginadas com até 100 registros por página |
| Auditoria | Datas de criação e atualização e históricos das mudanças de negócio |

### Perfis e permissões

| Recurso | `OPERADOR` | `GESTOR` | `LIDERANCA` |
|---|---|---|---|
| Cadastro e login | Público | Público | Público |
| Estratégias | Consulta | Consulta | Gestão completa |
| Ideias | Cria e administra as próprias ideias | Consulta e avaliação | Consulta |
| Projetos | Sem acesso | Gestão completa | Consulta |
| Dashboard | Sem acesso | Sem acesso | Consulta |
| Ranking | Consulta | Consulta | Consulta |
| Executar análise por IA | Não | Sim | Não |
| Consultar análise por IA | Não | Sim | Sim |

No cadastro público, a ausência de `codigoAcesso` cria um usuário `OPERADOR`. Os perfis `GESTOR` e `LIDERANCA` dependem dos códigos privados configurados pela administração. A role é sempre definida pelo backend.

## Arquitetura

```text
Cliente web ou mobile
        │
        │ HTTP/JSON + Bearer JWT
        ▼
Spring Web MVC Controllers (:8080)
        │
        ├── Spring Security ── autenticação e autorização por perfil
        │
        ▼
Services ── regras de negócio, auditoria e históricos
        │
        ├── Repositories ───────────────► MongoDB Atlas
        │
        └── GeminiRestClient ───────────► Gemini Interactions API
```

### Comunicação

- O cliente consome a API via HTTP e JSON.
- As rotas protegidas exigem o header `Authorization: Bearer <TOKEN_JWT>`.
- O JWT utiliza o algoritmo HS256, contém a role do usuário e expira em 1 hora por padrão.
- Controllers recebem e retornam DTOs; os documentos do MongoDB não são expostos diretamente.
- Erros seguem uma estrutura própria com status, mensagem e dados seguros, sem detalhes internos sensíveis.
- A API utiliza IDs `String` nos relacionamentos e não utiliza JPA, Hibernate, banco relacional, Lombok ou `@DBRef`.

## Estrutura de pastas

```text
inovagab-api/
├── .env.example                         # Modelo das variáveis de ambiente
├── pom.xml                              # Dependências e configuração Maven
├── mvnw                                 # Maven Wrapper para Linux/macOS
├── mvnw.cmd                             # Maven Wrapper para Windows
│
└── src/
    ├── main/
    │   ├── java/br/com/inovagab/api/
    │   │   ├── InovagabApiApplication.java  # Entry point da aplicação
    │   │   ├── config/                      # Segurança, MongoDB, JWT, Gemini e OpenAPI
    │   │   ├── controller/                  # Endpoints REST
    │   │   ├── dto/                         # Contratos de entrada e saída
    │   │   ├── exception/                   # Exceções e tratamento global de erros
    │   │   ├── ia/                          # Cliente REST, prompt e resposta da Gemini
    │   │   ├── model/                       # Documentos, enums e históricos
    │   │   ├── repository/                  # Acesso ao MongoDB
    │   │   ├── security/                    # Emissão de JWT e erros de segurança
    │   │   └── service/                     # Regras de negócio
    │   └── resources/
    │       └── application.properties       # Configuração da aplicação
    │
    └── test/java/br/com/inovagab/api/       # Testes unitários e de segurança
```

## Como executar

### Pré-requisitos

- JDK 21.
- Acesso a um cluster MongoDB Atlas.
- Chave da Gemini API, caso queira utilizar a análise por IA.
- Git. Não é necessário instalar o Maven, pois o projeto inclui o Maven Wrapper.

### Frontend

Este repositório contém apenas o backend. Um frontend web, mobile ou outro cliente HTTP deve consumir a API em:

```text
http://localhost:8080
```

No emulador Android, utilize `http://10.0.2.2:8080`, pois `localhost` aponta para o próprio emulador.

Após o login, envie o token recebido em todas as rotas protegidas:

```http
Authorization: Bearer <TOKEN_JWT>
```

### Backend

```powershell
# Clonar o repositório
git clone https://github.com/LucasFerdev/inovagabv2_api.git
cd inovagabv2_api

# Criar o arquivo local de ambiente a partir do modelo
Copy-Item .env.example .env
```

O Spring Boot não carrega arquivos `.env` automaticamente. Configure os valores no IntelliJ IDEA em **Run/Debug Configurations > Environment variables** ou carregue-os no terminal antes de iniciar a aplicação.

Variáveis utilizadas:

| Variável | Obrigatória | Descrição |
|---|---:|---|
| `MONGODB_URI` | Sim | URI completa do MongoDB Atlas |
| `JWT_SECRET` | Sim | Chave Base64 com pelo menos 256 bits |
| `JWT_EXPIRATION_SECONDS` | Não | Validade do JWT; padrão `3600` segundos |
| `CADASTRO_CODIGO_GESTOR` | Não | Código privado para cadastro de gestores |
| `CADASTRO_CODIGO_LIDERANCA` | Não | Código privado para cadastro de lideranças |
| `GEMINI_API_KEY` | Para IA | Chave de acesso à Gemini API |
| `GEMINI_MODEL` | Não | Modelo; padrão `gemini-3.5-flash-lite` |
| `GEMINI_BASE_URL` | Não | URL base do provedor |
| `GEMINI_TIMEOUT_SECONDS` | Não | Timeout da integração; padrão `20` segundos |

Com as variáveis configuradas no ambiente atual:

```powershell
# Iniciar a API no Windows
.\mvnw.cmd spring-boot:run
```

```bash
# Iniciar a API no Linux ou macOS
./mvnw spring-boot:run
```

Servidor disponível em `http://localhost:8080`.

Para executar a suíte de testes:

```powershell
.\mvnw.cmd clean test
```

Os testes automatizados não acessam o MongoDB Atlas nem a Gemini API real.

## Banco de dados

### MongoDB Atlas

A aplicação utiliza o banco `inovagab` e cria os índices necessários durante a inicialização.

| Coleção | Descrição |
|---|---|
| `usuarios` | Usuários, credenciais protegidas, perfil e estado da conta |
| `estrategias` | Estratégias de inovação e respectivos históricos |
| `ideias` | Ideias submetidas, avaliação, prioridade e análise de IA |
| `projetos` | Projetos originados de ideias, progresso e resultados |

### Configuração

1. Crie um cluster e o banco `inovagab` no MongoDB Atlas.
2. Crie um usuário com permissões de leitura, escrita e criação de índices.
3. Autorize somente os endereços de rede necessários.
4. Defina a URI completa exclusivamente em `MONGODB_URI`.
5. Nunca inclua credenciais em commits, logs ou capturas de tela.

Os relacionamentos entre coleções são feitos por IDs `String`. O controle otimista de concorrência utiliza versionamento dos documentos.

## API — Endpoints

### Autenticação

| Método | Rota | Descrição | Auth |
|---|---|---|---:|
| `POST` | `/api/auth/cadastro` | Criar conta | — |
| `POST` | `/api/auth/login` | Autenticar e emitir JWT | — |
| `GET` | `/api/auth/me` | Consultar usuário autenticado | ✅ |

Exemplo de cadastro de um operador:

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

### Estratégias

| Método | Rota | Descrição | Perfis |
|---|---|---|---|
| `POST` | `/api/estrategias` | Criar estratégia | `LIDERANCA` |
| `GET` | `/api/estrategias` | Listar com filtros e paginação | Todos |
| `GET` | `/api/estrategias/ativas` | Listar estratégias ativas | Todos |
| `GET` | `/api/estrategias/{id}` | Consultar por ID | Todos |
| `GET` | `/api/estrategias/{id}/historico` | Consultar histórico | Todos |
| `PUT` | `/api/estrategias/{id}` | Atualizar estratégia | `LIDERANCA` |
| `PATCH` | `/api/estrategias/{id}/ativar` | Ativar estratégia | `LIDERANCA` |
| `PATCH` | `/api/estrategias/{id}/desativar` | Desativar estratégia | `LIDERANCA` |
| `DELETE` | `/api/estrategias/{id}` | Arquivar estratégia | `LIDERANCA` |

Filtros disponíveis na listagem: `status`, `categoria`, `campanha`, `pagina` e `tamanho`.

### Ideias

| Método | Rota | Descrição | Perfis |
|---|---|---|---|
| `POST` | `/api/ideias` | Enviar ideia | `OPERADOR` |
| `GET` | `/api/ideias/minhas` | Listar ideias do usuário | `OPERADOR` |
| `GET` | `/api/ideias` | Listar ideias com filtros | `GESTOR`, `LIDERANCA` |
| `GET` | `/api/ideias/{id}` | Consultar por ID | Todos* |
| `GET` | `/api/ideias/{id}/historico` | Consultar histórico | Todos* |
| `PUT` | `/api/ideias/{id}` | Atualizar ideia própria | `OPERADOR` |
| `DELETE` | `/api/ideias/{id}` | Arquivar ideia própria | `OPERADOR` |
| `PATCH` | `/api/ideias/{id}/analisar` | Colocar ideia em análise | `GESTOR` |
| `PATCH` | `/api/ideias/{id}/priorizar` | Definir prioridade | `GESTOR` |
| `PATCH` | `/api/ideias/{id}/aprovar` | Aprovar ideia | `GESTOR` |
| `PATCH` | `/api/ideias/{id}/rejeitar` | Rejeitar ideia | `GESTOR` |

\* Operadores acessam somente as próprias ideias. Filtros disponíveis: `status`, `categoria`, `estrategiaId`, `prioridade`, `pagina` e `tamanho`.

### Projetos

| Método | Rota | Descrição | Perfis |
|---|---|---|---|
| `POST` | `/api/projetos` | Criar projeto | `GESTOR` |
| `GET` | `/api/projetos` | Listar com filtros e paginação | `GESTOR`, `LIDERANCA` |
| `GET` | `/api/projetos/{id}` | Consultar por ID | `GESTOR`, `LIDERANCA` |
| `GET` | `/api/projetos/{id}/historico` | Consultar histórico | `GESTOR`, `LIDERANCA` |
| `PUT` | `/api/projetos/{id}` | Atualizar projeto | `GESTOR` |
| `PATCH` | `/api/projetos/{id}/progresso` | Atualizar etapa e progresso | `GESTOR` |
| `PATCH` | `/api/projetos/{id}/resultados` | Registrar resultados | `GESTOR` |
| `PATCH` | `/api/projetos/{id}/concluir` | Concluir projeto | `GESTOR` |
| `DELETE` | `/api/projetos/{id}` | Cancelar projeto | `GESTOR` |

Filtros disponíveis na listagem: `status`, `etapa`, `estrategiaId`, `gestorId`, `prazo`, `pagina` e `tamanho`.

### Dashboard, ranking e IA

| Método | Rota | Descrição | Perfis |
|---|---|---|---|
| `GET` | `/api/dashboard/resumo` | Consultar resumo executivo | `LIDERANCA` |
| `GET` | `/api/dashboard/estrategias/{estrategiaId}` | Indicadores da estratégia | `LIDERANCA` |
| `GET` | `/api/dashboard/projetos/{projetoId}` | Indicadores do projeto | `LIDERANCA` |
| `GET` | `/api/ranking/colaboradores` | Consultar ranking | Todos |
| `POST` | `/api/ia/ideias/{ideiaId}/analisar?recalcular=false` | Gerar ou recuperar análise | `GESTOR` |
| `GET` | `/api/ia/ideias/{ideiaId}/analise` | Consultar análise persistida | `GESTOR`, `LIDERANCA` |

A ideia deve estar em `EM_ANALISE` para ser enviada à Gemini. A análise é persistida e reutilizada nas próximas consultas; use `recalcular=true` para gerar uma nova versão.

### Operação e documentação

| Método | Rota | Descrição | Auth |
|---|---|---|---:|
| `GET` | `/actuator/health` | Verificar a saúde da aplicação | — |
| `GET` | `/actuator/info` | Consultar informações da aplicação | — |
| `GET` | `/v3/api-docs` | Documento OpenAPI em JSON | — |
| `GET` | `/swagger-ui.html` | Interface interativa Swagger UI | — |

## Insomnia

O repositório ainda não inclui uma collection versionada. Para testar a API no Insomnia:

1. Crie um ambiente com `base_url` igual a `http://localhost:8080`.
2. Envie `POST {{ base_url }}/api/auth/cadastro` para criar um usuário.
3. Envie `POST {{ base_url }}/api/auth/login` e copie o token retornado.
4. Salve o valor em uma variável `token`.
5. Nas rotas protegidas, configure **Auth > Bearer Token** com `{{ token }}`.

Também é possível explorar e executar as rotas pela Swagger UI em `http://localhost:8080/swagger-ui.html`.

## Boas práticas

- **Arquitetura em camadas** — controllers, services, repositories, DTOs e configurações com responsabilidades separadas.
- **DTOs e validação** — documentos MongoDB não são expostos diretamente e entradas usam Bean Validation.
- **JWT stateless** — autenticação sem sessão de servidor e autorização por perfil em cada operação.
- **Senha protegida** — credenciais são armazenadas com hash e nunca retornadas pela API.
- **Segredos externos** — URI do banco, chave JWT, códigos de acesso e chave Gemini ficam em variáveis de ambiente.
- **Auditoria e histórico** — mudanças importantes registram autoria, data e ação realizada.
- **Concorrência otimista** — documentos versionados evitam sobrescritas silenciosas.
- **Índices MongoDB** — índices são criados na inicialização para os principais filtros e consultas.
- **IA com privacidade** — somente os dados necessários da ideia são enviados ao provedor; JWT, e-mail e IDs internos não são compartilhados.
- **Decisão humana** — a Gemini gera recomendações, mas não aprova, rejeita ou prioriza ideias automaticamente.
- **Testes automatizados** — serviços, segurança dos controllers, configurações e cliente Gemini possuem cobertura dedicada.
- **Documentação viva** — OpenAPI e Swagger UI refletem os contratos expostos pela aplicação.

> Nunca versione chaves da Gemini, URIs do MongoDB, senhas, códigos privados, tokens JWT ou outras credenciais.

## Autor

Lucas Fernando da Silva
