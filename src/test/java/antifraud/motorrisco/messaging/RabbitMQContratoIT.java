package antifraud.motorrisco.messaging;

import antifraud.motorrisco.config.RabbitMQConfig;
import antifraud.motorrisco.dto.TransacaoEventoDTO;
import antifraud.motorrisco.service.AnalisadorRiscoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.verify;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class RabbitMQContratoIT {

    @Container
    @ServiceConnection
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:4.1-management-alpine");

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private AnalisadorRiscoService analisadorRiscoService;

    @Test
    @DisplayName("Consumer desserializa o contrato do produtor sem depender de sua classe Java")
    void consumir_eventoDoProdutor_deveDesserializarContratoLocal() {
        EventoDoProdutor eventoDoProdutor = new EventoDoProdutor(
                UUID.fromString("7f3e4c2a-1b5d-4e8f-9a2c-3d6b7e1f4a8c"),
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                new BigDecimal("150.75"),
                "RESTAURANTE",
                "BRA",
                LocalDateTime.of(2026, 9, 8, 14, 30, 1),
                LocalDateTime.of(2026, 8, 20, 10, 15)
        );
        TransacaoEventoDTO eventoEsperado = new TransacaoEventoDTO(
                eventoDoProdutor.transacaoId(),
                eventoDoProdutor.contaId(),
                eventoDoProdutor.valor(),
                eventoDoProdutor.categoria(),
                eventoDoProdutor.codigoPais(),
                eventoDoProdutor.dataHora(),
                eventoDoProdutor.contaCriadaEm()
        );

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_TRANSACOES,
                RabbitMQConfig.ROUTING_KEY_RISCO,
                eventoDoProdutor
        );

        ArgumentCaptor<TransacaoEventoDTO> captor = ArgumentCaptor.forClass(TransacaoEventoDTO.class);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                verify(analisadorRiscoService).analisar(captor.capture()));

        assertThat(eventoDoProdutor.getClass()).isNotEqualTo(TransacaoEventoDTO.class);
        assertThat(captor.getValue()).isEqualTo(eventoEsperado);
    }

    private record EventoDoProdutor(
            UUID transacaoId,
            UUID contaId,
            BigDecimal valor,
            String categoria,
            String codigoPais,
            LocalDateTime dataHora,
            LocalDateTime contaCriadaEm
    ) {
    }
}
