package antifraud.motorrisco.consumer;

import antifraud.motorrisco.dto.TransacaoEventoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransacaoConsumer {

    @RabbitListener(queues = antifraud.motorrisico.config.RabbitMQConfig.FILA_ANALISE)
    public void consumir(TransacaoEventoDTO evento) {
        // AnalisadorRiscoService será chamado aqui posteriormente
    }
}
