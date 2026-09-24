package antifraud.motorrisco.messaging;

import antifraud.motorrisco.config.RabbitMQConfig;
import antifraud.motorrisco.enums.NivelRisco;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class RabbitMQContratoIT {

    @Container
    @ServiceConnection
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:4.1-management-alpine");

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @DisplayName("Fluxo RabbitMQ analisa contrato do produtor e publica contrato independente de resultado")
    void publicar_eventoDoProdutor_devePublicarResultadoCompleto() throws Exception {
        EventoDoProdutor evento = new EventoDoProdutor(
                UUID.fromString("7f3e4c2a-1b5d-4e8f-9a2c-3d6b7e1f4a8c"),
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                new BigDecimal("6000.00"),
                "ELETRONICOS",
                "USA",
                LocalDateTime.of(2026, 9, 8, 2, 30),
                LocalDateTime.of(2026, 8, 20, 10, 15)
        );

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_TRANSACOES,
                RabbitMQConfig.ROUTING_KEY_RISCO,
                evento
        );

        Message mensagem = rabbitTemplate.receive(RabbitMQConfig.FILA_RESULTADOS, 10_000);

        assertThat(mensagem).isNotNull();
        ResultadoDoConsumidor resultado = objectMapper.readValue(
                mensagem.getBody(), ResultadoDoConsumidor.class);
        assertThat(resultado.transacaoId()).isEqualTo(evento.transacaoId());
        assertThat(resultado.pontuacao()).isEqualTo(155);
        assertThat(resultado.nivel()).isEqualTo(NivelRisco.BLOQUEADA);
        assertThat(resultado.regrasDisparadas()).containsExactly(
                "VALOR_ALTO", "CONTA_NOVA", "HORARIO_SUSPEITO",
                "VALOR_MUITO_ALTO_CONTA_NOVA", "PAIS_ESTRANGEIRO");
        assertThat(resultado.analisadoEm()).isNotNull();
    }

    private record EventoDoProdutor(UUID transacaoId, UUID contaId, BigDecimal valor,
                                    String categoria, String codigoPais, LocalDateTime dataHora,
                                    LocalDateTime contaCriadaEm) {
    }

    private record ResultadoDoConsumidor(UUID transacaoId, int pontuacao, NivelRisco nivel,
                                         List<String> regrasDisparadas, LocalDateTime analisadoEm) {
    }
}
