# motor-risco

Microsserviço orientado a eventos que recebe transações pelo RabbitMQ, aplica uma cadeia de regras antifraude e calcula uma classificação de risco. Ele não expõe endpoint REST para iniciar análises e não acessa dados internos ou o banco do serviço produtor.

## Fluxo

```text
transacoes.analise
        ↓
TransacaoConsumer
        ↓
AnalisadorRiscoService
        ↓
ContextoAnalise → cadeia de RegraRisco
        ↓
ResultadoAnaliseDTO
        ↓
publicação pendente de contrato e topologia
```

O consumer somente registra os identificadores da mensagem e delega. O service cria um único contexto, executa todas as regras e classifica a soma final. A ordem das regras é explícita por `@Order` e a mesma instância de contexto percorre toda a cadeia.

## Contrato de entrada

Topologia RabbitMQ:

- exchange direct durável: `transacoes.exchange`
- fila durável: `transacoes.analise`
- routing key: `transacoes.risco`

Payload:

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

`dataHora` é o horário de criação/registro da transação no servidor produtor. Como `LocalDateTime` não carrega fuso, o consumidor não aplica conversão arbitrária. O conversor ignora a identidade da classe Java informada pelo produtor e desserializa no `TransacaoEventoDTO` local.

## Regras implementadas

| Regra | Condição | Pontos |
|---|---|---:|
| `RegraValorAlto` | valor maior que 5000 | 30 |
| `RegraContaNova` | conta com menos de 30 dias no instante do evento e valor maior que 1000 | 35 |
| `RegraHorarioSuspeito` | horário a partir de 00:00 e antes de 06:00 e valor maior que 500 | 20 |
| `RegraValorMuitoAltoContaNova` | conta com menos de 30 dias no instante do evento e valor maior que 5000 | 45 |
| `RegraPaisEstrangeiro` | `codigoPais` diferente de `BRA` | 25 |

As regras são cumulativas e executadas nesta ordem: `RegraValorAlto`, `RegraContaNova`, `RegraHorarioSuspeito`, `RegraValorMuitoAltoContaNova` e `RegraPaisEstrangeiro`. Os cálculos de idade usam `contaCriadaEm` e `dataHora`, sem consultar o relógio atual.

Classificação final:

| Pontuação | Resultado |
|---|---|
| 0–39 | `APROVADA` |
| 40–69 | `SINALIZADA` |
| 70+ | `BLOQUEADA` |

## Pendências

- decidir se `RegraFrequencia` será incluída e onde seu histórico será armazenado;
- confirmar o contrato de saída e a exchange/routing key da publicação do resultado; nenhum publisher é criado antes disso.

## Executar

Requisitos: Java 21 e Docker para o teste de integração RabbitMQ.

```bash
./mvnw test
./mvnw verify
```

No Windows:

```powershell
.\mvnw.cmd test
.\mvnw.cmd verify
```

Testes unitários não iniciam contexto Spring. O `RabbitMQContratoIT` usa RabbitMQ real via Testcontainers e é executado pelo Maven Failsafe durante `verify`; sem Docker, ele é explicitamente ignorado.
