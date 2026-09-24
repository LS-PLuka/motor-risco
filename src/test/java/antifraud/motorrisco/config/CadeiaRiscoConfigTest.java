package antifraud.motorrisco.config;

import antifraud.motorrisco.model.ContextoAnalise;
import antifraud.motorrisco.regras.RegraContaNova;
import antifraud.motorrisco.regras.RegraRisco;
import antifraud.motorrisco.regras.RegraValorAlto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static antifraud.motorrisco.EventoFixture.evento;
import static org.assertj.core.api.Assertions.assertThat;

class CadeiaRiscoConfigTest {

    @Test
    @DisplayName("Configuração ordena a cadeia explicitamente e acumula regras")
    void cadeiaRisco_regrasEmOrdemInversa_deveOrdenarEExecutarTodas() {
        RegraRisco cadeia = new CadeiaRiscoConfig().cadeiaRisco(
                List.of(new RegraContaNova(), new RegraValorAlto()));
        LocalDateTime dataHora = LocalDateTime.of(2026, 9, 8, 14, 30);
        ContextoAnalise contexto = new ContextoAnalise(evento(
                new BigDecimal("6000"), dataHora, dataHora.minusDays(10)));

        cadeia.analisar(contexto);

        assertThat(contexto.getPontuacao()).isEqualTo(65);
        assertThat(contexto.getRegrasDisparadas()).containsExactly("VALOR_ALTO", "CONTA_NOVA");
    }
}
