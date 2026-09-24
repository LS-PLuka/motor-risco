# AGENTS.md

## Escopo

Estas instruções valem para todo o repositório `motor-risco`. Antes de alterar código, leia o estado real do projeto e preserve mudanças preexistentes. O código e os contratos presentes no repositório são a fonte da verdade. Não faça refatorações fora do escopo solicitado.

## Responsabilidade do serviço

O `motor-risco` é um microsserviço orientado a eventos. Ele consome eventos de transação, aplica regras antifraude, acumula uma pontuação, classifica o resultado e publica o resultado da análise.

Não crie Controller REST para iniciar análises. Não replique autenticação JWT, autorização, entidades, DTOs, repositories ou acesso ao banco do `servico-transacao`. O consumidor usa seu próprio contrato local e não depende do nome da classe Java do produtor.

Fluxo esperado:

`TransacaoConsumer -> AnalisadorRiscoService -> ContextoAnalise -> cadeia de RegraRisco -> ResultadoAnaliseDTO -> ResultadoAnalisePublisher`

## Contrato de entrada e RabbitMQ

O evento de entrada é representado localmente por `TransacaoEventoDTO` com estes nomes e tipos exatos:

- `transacaoId`: `UUID`
- `contaId`: `UUID`
- `valor`: `BigDecimal`
- `categoria`: `String`
- `codigoPais`: `String`
- `dataHora`: `LocalDateTime`
- `contaCriadaEm`: `LocalDateTime`

`dataHora` representa atualmente o horário de criação/registro da transação no servidor produtor. Não atribua a ele outra semântica nem aplique fuso horário arbitrário.

Topologia de entrada:

- exchange direct durável: `transacoes.exchange`
- fila durável: `transacoes.analise`
- routing key: `transacoes.risco`

Centralize os nomes RabbitMQ em configuração. O conversor JSON do consumidor deve aceitar mensagens produzidas com `Jackson2JsonMessageConverter` sem depender do header com o nome Java da classe do produtor. Alterações nessa interoperabilidade exigem teste de integração com RabbitMQ real.

Contrato e topologia de saída:

- exchange direct durável: `risco.exchange`
- fila durável: `risco.resultados`
- routing key: `risco.resultado`
- `ResultadoAnaliseDTO`: `transacaoId` (`UUID`), `pontuacao` (`int`), `nivel` (`NivelRisco`), `regrasDisparadas` (`List<String>`) e `analisadoEm` (`LocalDateTime`)

`analisadoEm` representa o momento em que o motor concluiu a análise. O contrato JSON não depende da classe Java do serviço consumidor.

## Responsabilidades dos componentes

- `TransacaoConsumer`: recebe o DTO, registra somente identificadores úteis e delega imediatamente ao service. Não contém regras, classificação ou montagem da cadeia.
- `AnalisadorRiscoService`: cria o contexto, executa toda a cadeia, classifica o score final, cria o resultado e delega sua publicação.
- `ContextoAnalise`: mantém o mesmo evento, a pontuação acumulada e os nomes das regras disparadas. Expõe operações simples para registrar contribuições sem permitir substituição do contexto.
- `RegraRisco`: avalia somente sua condição, adiciona pontos e identificação quando disparada e encaminha o mesmo contexto à próxima regra. Nenhuma regra classifica ou publica. A ordem da cadeia é explícita e determinística e a cadeia não para após um disparo.
- `ResultadoAnalisePublisher`: encapsula somente a chamada ao `RabbitTemplate`; não classifica nem recalcula pontuação.

## Regras e classificação confirmadas

- `RegraValorAlto`: `valor > 5000`, adiciona 30 pontos. Exatamente 5000 não dispara.
- `RegraContaNova`: conta com menos de 30 dias no instante de `dataHora` e `valor > 1000`, adiciona 35 pontos. Exatamente 30 dias ou exatamente 1000 não disparam. Use o horário do evento, nunca o relógio atual.
- `RegraHorarioSuspeito`: horário a partir de 00:00 e antes de 06:00 e `valor > 500`, adiciona 20 pontos. Exatamente 06:00 ou exatamente 500 não disparam.
- `RegraValorMuitoAltoContaNova`: conta com menos de 30 dias no instante de `dataHora` e `valor > 5000`, adiciona 45 pontos. Exatamente 30 dias ou exatamente 5000 não disparam. Use o horário do evento, nunca o relógio atual.
- `RegraPaisEstrangeiro`: `codigoPais` diferente de `BRA`, adiciona 25 pontos.

As cinco regras são cumulativas e executadas nesta ordem: `RegraValorAlto`, `RegraContaNova`, `RegraHorarioSuspeito`, `RegraValorMuitoAltoContaNova` e `RegraPaisEstrangeiro`.

Classificação somente após toda a cadeia:

- 0 a 39: `APROVADA`
- 40 a 69: `SINALIZADA`
- 70 ou mais: `BLOQUEADA`

## Pendências arquiteturais

Não implemente nem resolva silenciosamente estas decisões:

- inclusão ou não da `RegraFrequencia`;
- armazenamento do histórico necessário à frequência (memória, PostgreSQL, Redis ou outra estratégia);

Não invente exchanges, filas, routing keys, regras ou persistência. Atualize este documento e o README quando essas decisões forem confirmadas.

## Padrão de código

- Java 21, Maven, Spring Boot, Spring AMQP, Lombok somente quando eliminar boilerplate real.
- Nomes de classes, métodos, campos e testes em português e indentação de quatro espaços.
- Uma classe pública por arquivo; dependências `private final`; injeção por construtor com `@RequiredArgsConstructor`.
- DTOs imutáveis como `record`; services concentram regras de negócio; consumidores e publishers permanecem finos.
- Logs com `@Slf4j` e placeholders `{}`. Prefira `transacaoId` e `contaId`; não registre payloads completos.
- Não use field injection, camadas genéricas sem responsabilidade, interfaces com uma única implementação sem necessidade, JavaDoc ou comentários que apenas repitam código.
- Não adicione Web, Security, JWT, OpenAPI, JPA, PostgreSQL ou outras dependências sem requisito concreto.

## Testes e build

- Use JUnit 5 e Mockito; prefira `@ExtendWith(MockitoExtension.class)`, `@Mock` e `@InjectMocks` em unidades com dependências.
- Nomeie testes como `metodo_cenario_resultado`, use `@DisplayName` em português e Arrange/Act/Assert quando ajudar a leitura.
- Teste cada regra isoladamente, abaixo, exatamente e acima dos limites, além das fronteiras temporais.
- Verifique pontuação, nome registrado, contexto inalterado quando não dispara, encaminhamento da mesma instância e acumulação entre regras.
- Teste a classificação nos scores 0, 39, 40, 69, 70 e acima de 70.
- Consumer é testado como adaptador fino; unidades não carregam Spring sem necessidade.
- Integrações usam RabbitMQ real com Testcontainers, têm sufixo `*IT` e precisam provar a desserialização do JSON e dos headers produzidos pelo contrato real.
- O comando completo de validação é `mvn verify` ou `./mvnw verify`. Testes unitários isolados usam `mvn test` ou `./mvnw test`. No Windows, use `mvnw.cmd verify` e `mvnw.cmd test`.
- Informe no resultado qualquer teste que não tenha sido executado ou que dependa de Docker/infraestrutura indisponível.

Não remova testes para fazer o build passar e não execute commits, branches, merges, push ou outras operações Git que alterem o histórico.
