package antifraud.motorrisco.consumer;

import antifraud.motorrisco.dto.TransacaoEventoDTO;
import antifraud.motorrisco.service.AnalisadorRiscoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static antifraud.motorrisco.config.RabbitMQConfig.FILA_ANALISE;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransacaoConsumer {

    private final AnalisadorRiscoService analisadorRiscoService;

    @RabbitListener(queues = FILA_ANALISE)
    public void consumir(TransacaoEventoDTO evento) {
        log.info("Recebida transação para análise. transacaoId={}, contaId={}",
                evento.transacaoId(), evento.contaId());
        analisadorRiscoService.analisar(evento);
    }
}
