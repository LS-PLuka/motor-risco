package antifraud.motorrisco.publisher;

import antifraud.motorrisco.dto.ResultadoAnaliseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static antifraud.motorrisco.config.RabbitMQConfig.EXCHANGE_RISCO;
import static antifraud.motorrisco.config.RabbitMQConfig.ROUTING_KEY_RESULTADO;

@Component
@RequiredArgsConstructor
public class ResultadoAnalisePublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publicar(ResultadoAnaliseDTO resultado) {
        rabbitTemplate.convertAndSend(EXCHANGE_RISCO, ROUTING_KEY_RESULTADO, resultado);
    }
}
