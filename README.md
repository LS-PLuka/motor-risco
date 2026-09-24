# motor-risco

[![CI](https://github.com/LS-PLuka/motor-risco/actions/workflows/ci.yml/badge.svg)](https://github.com/LS-PLuka/motor-risco/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.14-6DB33F?style=flat-square)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.13-FF6600?style=flat-square)

Motor de análise de risco do **antifraud-system**. Consome transações de forma assíncrona pelo RabbitMQ, aplica uma cadeia de regras antifraude, acumula uma pontuação de risco e classifica cada transação como `APROVADA`, `SINALIZADA` ou `BLOQUEADA`.

Este serviço **não recebe transações via REST, não autentica usuários e não acessa o banco do `servico-transacao`**. Sua responsabilidade começa quando uma mensagem chega em `transacoes.analise` e termina quando o resultado da análise é publicado em `risco.resultados`.

---

## Índice

- [Arquitetura](#arquitetura)
- [Contrato de entrada](#contrato-de-entrada)
- [Contrato de saída](#contrato-de-saída)
- [Regras de risco](#regras-de-risco)
- [Classificação](#classificação)
- [Decisões de arquitetura e trade-offs](#decisões-de-arquitetura-e-trade-offs)
- [Testes](#testes)
- [Stack](#stack)
- [Configuração](#configuração)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Parte de um sistema maior](#parte-de-um-sistema-maior)

---

## Arquitetura

```text
servico-transacao
        │
        ▼
transacoes.exchange
routing key: transacoes.risco
        │
        ▼
transacoes.analise
        │
        ▼
┌──────────────────────────────────────────┐
│               motor-risco                │
│                                          │
│           TransacaoConsumer              │
│                  │                       │
│                  ▼                       │
│        AnalisadorRiscoService            │
│                  │                       │
│                  ▼                       │
│           ContextoAnalise                │
│                  │                       │
│                  ▼                       │
│      Chain of Responsibility             │
│                  │                       │
│      ┌───────────┼───────────┐           │
│      ▼           ▼           ▼           │
│   regras       score     classificação   │
│                  │                       │
│                  ▼                       │
│        ResultadoAnaliseDTO               │
│                  │                       │
│                  ▼                       │
│       ResultadoAnalisePublisher          │
└──────────────────┼───────────────────────┘
                   │
                   ▼
             risco.exchange
        routing key: risco.resultado
                   │
                   ▼
             risco.resultados
                   │
                   ▼
           servico-auditoria
```

O `TransacaoConsumer` é deliberadamente fino: registra os identificadores relevantes da mensagem e delega a análise.

O `AnalisadorRiscoService` cria um único `ContextoAnalise`, executa toda a cadeia, calcula a classificação final, cria o `ResultadoAnaliseDTO` e delega sua publicação ao `ResultadoAnalisePublisher`.

As regras não conhecem RabbitMQ e o publisher não conhece regras de negócio.

---

## Contrato de entrada

A topologia consumida pelo motor é declarada no `RabbitMQConfig`:

| Componente | Nome | Tipo |
|---|---|---|
| Exchange | `transacoes.exchange` | `direct`, durável |
| Routing key | `transacoes.risco` | — |
| Fila | `transacoes.analise` | `durable` |

Payload recebido (`TransacaoEventoDTO`):

```json
{
  "transacaoId": "7f3e4c2a-1b5d-4e8f-9a2c-3d6b7e1f4a8c",
  "contaId": "550e8400-e29b-41d4-a716-446655440000",
  "valor": 150.75,
  "categoria": "RESTAURANTE",
  "codigoPais": "BRA",
  "dataHora": "2026-09-08T14:30:01",
  "contaCriadaEm": "2026-08-20T10:15:00"
}
```

| Campo | Tipo | Uso |
|---|---|---|
| `transacaoId` | `UUID` | identifica a transação analisada |
| `contaId` | `UUID` | identifica a conta responsável |
| `valor` | `BigDecimal` | utilizado pelas regras de valor |
| `categoria` | `String` | informação de contexto da transação |
| `codigoPais` | `String` | utilizado na regra de país estrangeiro |
| `dataHora` | `LocalDateTime` | instante de referência da transação |
| `contaCriadaEm` | `LocalDateTime` | permite calcular a idade da conta |

`dataHora` é o horário de criação/registro da transação definido pelo serviço produtor. Como `LocalDateTime` não carrega fuso horário, o consumidor não aplica conversão arbitrária.

O motor possui seu próprio `TransacaoEventoDTO`. A desserialização não depende da classe Java utilizada pelo produtor, evitando acoplamento entre os repositórios.

---

## Contrato de saída

Após concluir a análise, o motor publica o resultado em uma topologia RabbitMQ separada:

| Componente | Nome | Tipo |
|---|---|---|
| Exchange | `risco.exchange` | `direct`, durável |
| Routing key | `risco.resultado` | — |
| Fila | `risco.resultados` | `durable` |

Payload publicado (`ResultadoAnaliseDTO`):

```json
{
  "transacaoId": "7f3e4c2a-1b5d-4e8f-9a2c-3d6b7e1f4a8c",
  "pontuacao": 155,
  "nivel": "BLOQUEADA",
  "regrasDisparadas": [
    "VALOR_ALTO",
    "CONTA_NOVA",
    "HORARIO_SUSPEITO",
    "VALOR_MUITO_ALTO_CONTA_NOVA",
    "PAIS_ESTRANGEIRO"
  ],
  "analisadoEm": "2026-09-24T10:30:00"
}
```

| Campo | Tipo | Descrição |
|---|---|---|
| `transacaoId` | `UUID` | transação à qual a decisão pertence |
| `pontuacao` | `int` | score acumulado pelas regras |
| `nivel` | `NivelRisco` | `APROVADA`, `SINALIZADA` ou `BLOQUEADA` |
| `regrasDisparadas` | `List<String>` | regras que contribuíram para o score |
| `analisadoEm` | `LocalDateTime` | momento em que o motor concluiu a análise |

`ResultadoAnalisePublisher` possui apenas responsabilidade de mensageria. Ele recebe o resultado já calculado pelo service e o publica no RabbitMQ.

Assim como no contrato de entrada, o consumidor futuro pode desserializar esse JSON para seu próprio DTO local sem compartilhar classes Java com o `motor-risco`.

---

## Regras de risco

O motor utiliza **Chain of Responsibility** para executar cinco regras em ordem determinística.

| Ordem | Regra | Condição | Pontos |
|---:|---|---|---:|
| 1 | `RegraValorAlto` | valor > R$ 5.000 | +30 |
| 2 | `RegraContaNova` | conta com menos de 30 dias e valor > R$ 1.000 | +35 |
| 3 | `RegraHorarioSuspeito` | `00:00 <= horário < 06:00` e valor > R$ 500 | +20 |
| 4 | `RegraValorMuitoAltoContaNova` | conta com menos de 30 dias e valor > R$ 5.000 | +45 |
| 5 | `RegraPaisEstrangeiro` | `codigoPais != "BRA"` | +25 |

As regras são cumulativas. Uma regra disparada **não interrompe a cadeia**.

A idade da conta é calculada utilizando:

```text
contaCriadaEm → dataHora
```

e nunca `LocalDateTime.now()`.

Isso torna a análise determinística: reprocessar o mesmo evento no futuro não altera a idade da conta considerada originalmente.

### Exemplo

Uma transação com:

```text
valor:          R$ 6.000
idade da conta: 10 dias
horário:        03:00
país:           USA
```

dispara:

```text
RegraValorAlto                 +30
RegraContaNova                 +35
RegraHorarioSuspeito           +20
RegraValorMuitoAltoContaNova   +45
RegraPaisEstrangeiro           +25
                               ───
Total                          155
```

Resultado:

```text
BLOQUEADA
```

---

## Classificação

A classificação acontece somente depois que toda a cadeia foi executada.

| Pontuação | Resultado | Descrição |
|---|---|---|
| 0–39 | `APROVADA` | risco baixo |
| 40–69 | `SINALIZADA` | risco intermediário |
| 70+ | `BLOQUEADA` | risco elevado |

Nenhuma regra individual decide o resultado da transação.

Cada regra contribui apenas com pontos para o `ContextoAnalise`; a classificação final é responsabilidade do `AnalisadorRiscoService`.

---

## Decisões de arquitetura e trade-offs

### 1. Motor exclusivamente orientado a eventos

O serviço não expõe endpoint REST para iniciar análises.

**Por quê:** a transação já foi recebida e persistida pelo `servico-transacao`. Fazer o motor participar da requisição original criaria uma dependência síncrona entre os microsserviços.

**Custo aceito:** o resultado da análise não faz parte da resposta imediata enviada ao cliente.

---

### 2. Nenhum acesso ao banco do produtor

O `motor-risco` não acessa PostgreSQL nem dados internos do `servico-transacao`.

Informações necessárias à análise, como `contaCriadaEm`, fazem parte do próprio evento.

**Por quê:** compartilhar banco entre microsserviços criaria acoplamento de schema e eliminaria parte da independência entre os serviços.

**Custo aceito:** o evento precisa transportar todos os dados necessários às regras stateless atuais.

---

### 3. Chain of Responsibility

Cada regra possui uma única responsabilidade: verificar sua condição e, quando necessário, registrar sua pontuação no contexto.

A ordem da cadeia é explícita através de `@Order`.

**Por quê:** novas regras podem ser adicionadas sem concentrar todas as condições dentro do `AnalisadorRiscoService`.

**Custo aceito:** a composição e a ordem da cadeia tornam-se parte importante da configuração do serviço.

---

### 4. Score cumulativo

Todas as regras percorrem o mesmo `ContextoAnalise`.

Uma regra disparada não impede a execução das seguintes.

**Por quê:** o risco é determinado pela combinação dos sinais encontrados, e não pela primeira condição verdadeira.

Isso permite, por exemplo, que uma única transação acumule os 155 pontos máximos das cinco regras atuais.

---

### 5. Tempo do evento como referência

As regras temporais utilizam `dataHora`, e a idade da conta utiliza `contaCriadaEm` em relação a esse mesmo instante.

**Por quê:** usar o relógio atual tornaria o resultado dependente do momento de processamento.

O mesmo evento deve produzir a mesma análise mesmo quando reprocessado posteriormente.

---

### 6. Contratos independentes entre serviços

O motor possui DTOs próprios tanto para consumo quanto para publicação.

**Por quê:** `servico-transacao`, `motor-risco` e `servico-auditoria` são aplicações independentes. Compartilhar classes Java entre elas criaria acoplamento de implementação.

A integração acontece através do contrato JSON publicado no RabbitMQ.

---

## Testes

O comando principal de validação é:

```bash
./mvnw verify
```

No Windows:

```powershell
.\mvnw.cmd verify
```

`test` executa somente os testes unitários:

```bash
./mvnw test
```

Os testes de integração seguem o sufixo `*IT` e são executados pelo Maven Failsafe durante `verify`.

### Testes unitários

Os testes unitários cobrem:

- cada regra de risco isoladamente;
- limites exatos de valores;
- limites de horário;
- limite de 30 dias da conta;
- país nacional e estrangeiro;
- ordem da Chain;
- acúmulo de pontuação;
- regras disparadas;
- classificações `APROVADA`, `SINALIZADA` e `BLOQUEADA`;
- orquestração do `AnalisadorRiscoService`;
- publicação do `ResultadoAnaliseDTO`.

Exemplos de fronteira:

```text
R$ 5.000,00  → RegraValorAlto não dispara
R$ 5.000,01  → dispara

R$ 500,00    → RegraHorarioSuspeito não dispara
R$ 500,01    → pode disparar

05:59:59     → dentro da janela suspeita
06:00:00     → fora da janela

29 dias      → conta nova
30 dias      → não é conta nova

BRA          → RegraPaisEstrangeiro não dispara
USA          → dispara
```

### Teste de integração

`RabbitMQContratoIT` utiliza RabbitMQ real através de Testcontainers.

O teste valida o fluxo de mensageria e os contratos campo a campo, em vez de apenas verificar chamadas internas.

O fluxo exercitado é:

```text
TransacaoEventoDTO
        ↓
transacoes.analise
        ↓
TransacaoConsumer
        ↓
AnalisadorRiscoService
        ↓
Chain
        ↓
ResultadoAnalisePublisher
        ↓
risco.resultados
```

O Testcontainers exige Docker disponível no ambiente e é executado pelo Maven Failsafe durante:

```bash
./mvnw verify
```

### CI

O GitHub Actions executa a validação em pushes e pull requests direcionados a `develop` e `main`.

A etapa principal utiliza:

```bash
mvn -B verify
```

Com isso, a CI executa tanto os testes unitários quanto os testes de integração RabbitMQ com Testcontainers.

---

## Stack

| Tecnologia | Versão | Papel |
|---|---|---|
| Java | 21 | LTS |
| Spring Boot | 3.5.14 | Framework base |
| Spring AMQP | — | Consumo e publicação RabbitMQ |
| RabbitMQ | 3.13 | Broker de mensagens |
| Lombok | 1.18.46 | Redução de boilerplate |
| JUnit 5 + Mockito | — | Testes unitários |
| Testcontainers | 1.20.4 | Testes de integração |
| Maven Failsafe | — | Separação unit / integração |
| GitHub Actions | — | Integração contínua |

`BigDecimal` é utilizado para valores monetários, `UUID` para identificadores e `record` para DTOs quando adequado.

O serviço não utiliza:

- Spring Web;
- Spring Security;
- Spring Data JPA;
- PostgreSQL;
- MongoDB.

---

## Configuração

O `motor-risco` não possui banco de dados próprio.

Sua única dependência externa em runtime é o RabbitMQ.

| Variável | Default | Descrição |
|---|---|---|
| `SERVER_PORT` | `8081` | Porta da aplicação |
| `RABBITMQ_HOST` | `localhost` | Host do broker |
| `RABBITMQ_PORT` | `5672` | Porta AMQP |
| `RABBITMQ_USERNAME` | `guest` | Usuário do broker |
| `RABBITMQ_PASSWORD` | `guest` | Senha do broker |

Para executar a aplicação localmente, o RabbitMQ precisa estar disponível.

```bash
./mvnw spring-boot:run
```

No Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

O microsserviço possui um `Dockerfile` próprio com build multi-stage. Para gerar a imagem:

```bash
docker build -t motor-risco .
```

Para executar a imagem isoladamente, informe um RabbitMQ acessível pelo container:

```bash
docker run --rm \
  -p 8081:8081 \
  -e RABBITMQ_HOST=<host-do-rabbitmq> \
  motor-risco
```

O `motor-risco` não possui Docker Compose próprio. Sua execução integrada com RabbitMQ e os demais serviços acontece pelo Compose do repositório central `antifraud-system`, que configurará `RABBITMQ_HOST=rabbitmq`.

A orquestração completa de PostgreSQL, RabbitMQ, MongoDB e dos três microsserviços pertence ao repositório central `antifraud-system`.

---

## Estrutura do projeto

```text
src/main/java/antifraud/motorrisco/
├── config/           # RabbitMQ e composição da Chain
├── consumer/         # consumo de transações
├── dto/              # contratos RabbitMQ de entrada e saída
├── enums/            # NivelRisco
├── model/            # ContextoAnalise
├── publisher/        # publicação do resultado
├── regras/           # Chain e regras antifraude
├── service/          # orquestração da análise
└── MotorRiscoApplication.java

src/test/java/antifraud/motorrisco/
├── config/           # composição e ordem da Chain
├── integration/      # RabbitMQ real com Testcontainers
├── publisher/        # publicação do resultado
├── regras/           # testes de fronteira das regras
└── service/          # score, classificação e orquestração
```

A direção principal de dependência é:

```text
consumer
   ↓
service
   ├── chain/regras
   └── publisher
```

O `consumer` não possui regra de negócio.

As regras não conhecem mensageria.

O publisher não calcula risco.

---

## Parte de um sistema maior

| Repositório | Papel |
|---|---|
| [antifraud-system](https://github.com/LS-PLuka/antifraud-system) | Orquestração e documentação geral |
| [servico-transacao](https://github.com/LS-PLuka/servico-transacao) | Entrada, validação, persistência e publicação das transações |
| **motor-risco** | **Este repositório** — análise, classificação e publicação do risco |
| [servico-auditoria](https://github.com/LS-PLuka/servico-auditoria) | Persistência do histórico das decisões |

Fluxo completo:

```text
Cliente
   ↓ REST
servico-transacao
   ↓
transacoes.analise
   ↓
motor-risco
   ↓
risco.resultados
   ↓
servico-auditoria
   ↓
MongoDB
```

Nenhum microsserviço acessa diretamente o banco de outro e não existe comunicação HTTP entre os serviços internos.

---

Desenvolvido por [Pedro Luka](https://github.com/LS-PLuka).