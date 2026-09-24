package antifraud.motorrisco.consumer;

import antifraud.motorrisco.dto.TransacaoEventoDTO;
import antifraud.motorrisco.service.AnalisadorRiscoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static antifraud.motorrisco.EventoFixture.evento;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransacaoConsumerTest {

    @Mock
    private AnalisadorRiscoService analisadorRiscoService;

    @InjectMocks
    private TransacaoConsumer consumer;

    @Test
    @DisplayName("Consumer delega o evento recebido ao service")
    void consumir_eventoValido_deveDelegarParaService() {
        LocalDateTime dataHora = LocalDateTime.of(2026, 9, 8, 14, 30);
        TransacaoEventoDTO transacao = evento(
                new BigDecimal("150.75"), dataHora, dataHora.minusDays(20));

        consumer.consumir(transacao);

        verify(analisadorRiscoService).analisar(transacao);
    }
}
