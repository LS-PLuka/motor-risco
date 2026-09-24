package antifraud.motorrisco.publisher;

import antifraud.motorrisco.dto.ResultadoAnaliseDTO;
import antifraud.motorrisco.enums.NivelRisco;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static antifraud.motorrisco.config.RabbitMQConfig.EXCHANGE_RISCO;
import static antifraud.motorrisco.config.RabbitMQConfig.ROUTING_KEY_RESULTADO;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ResultadoAnalisePublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private ResultadoAnalisePublisher publisher;

    @Test
    @DisplayName("Publisher envia o resultado para a topologia de saída")
    void publicar_resultadoValido_deveEnviarParaExchangeERoutingKeyCorretas() {
        ResultadoAnaliseDTO resultado = new ResultadoAnaliseDTO(
                UUID.fromString("7f3e4c2a-1b5d-4e8f-9a2c-3d6b7e1f4a8c"),
                40,
                NivelRisco.SINALIZADA,
                List.of("REGRA_TESTE"),
                LocalDateTime.of(2026, 9, 24, 10, 30)
        );

        publisher.publicar(resultado);

        verify(rabbitTemplate).convertAndSend(EXCHANGE_RISCO, ROUTING_KEY_RESULTADO, resultado);
    }
}
