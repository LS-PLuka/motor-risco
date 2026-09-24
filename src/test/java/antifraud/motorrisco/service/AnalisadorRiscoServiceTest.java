package antifraud.motorrisco.service;

import antifraud.motorrisco.dto.ResultadoAnaliseDTO;
import antifraud.motorrisco.dto.TransacaoEventoDTO;
import antifraud.motorrisco.enums.NivelRisco;
import antifraud.motorrisco.model.ContextoAnalise;
import antifraud.motorrisco.regras.RegraRisco;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static antifraud.motorrisco.EventoFixture.evento;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AnalisadorRiscoServiceTest {

    @Mock
    private RegraRisco cadeiaRisco;

    @InjectMocks
    private AnalisadorRiscoService service;

    @Test
    @DisplayName("Análise executa a cadeia e monta o resultado final")
    void analisar_eventoValido_deveExecutarCadeiaEClassificarResultado() {
        TransacaoEventoDTO transacao = criarEvento();
        doAnswer(invocacao -> {
            ContextoAnalise contexto = invocacao.getArgument(0);
            contexto.registrarRegraDisparada(40, "REGRA_TESTE");
            return null;
        }).when(cadeiaRisco).analisar(any(ContextoAnalise.class));

        ResultadoAnaliseDTO resultado = service.analisar(transacao);

        verify(cadeiaRisco).analisar(any(ContextoAnalise.class));
        assertThat(resultado.transacaoId()).isEqualTo(transacao.transacaoId());
        assertThat(resultado.contaId()).isEqualTo(transacao.contaId());
        assertThat(resultado.pontuacao()).isEqualTo(40);
        assertThat(resultado.classificacao()).isEqualTo(NivelRisco.SINALIZADA);
        assertThat(resultado.regrasDisparadas()).containsExactly("REGRA_TESTE");
    }

    private TransacaoEventoDTO criarEvento() {
        LocalDateTime dataHora = LocalDateTime.of(2026, 9, 8, 14, 30);
        return evento(new BigDecimal("150.75"), dataHora, dataHora.minusDays(60));
    }
}
