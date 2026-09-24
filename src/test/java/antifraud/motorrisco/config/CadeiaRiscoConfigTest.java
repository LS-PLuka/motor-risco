package antifraud.motorrisco.config;

import antifraud.motorrisco.enums.NivelRisco;
import antifraud.motorrisco.model.ContextoAnalise;
import antifraud.motorrisco.regras.RegraContaNova;
import antifraud.motorrisco.regras.RegraHorarioSuspeito;
import antifraud.motorrisco.regras.RegraPaisEstrangeiro;
import antifraud.motorrisco.regras.RegraRisco;
import antifraud.motorrisco.regras.RegraValorAlto;
import antifraud.motorrisco.regras.RegraValorMuitoAltoContaNova;
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
                List.of(
                        new RegraPaisEstrangeiro(),
                        new RegraValorMuitoAltoContaNova(),
                        new RegraHorarioSuspeito(),
                        new RegraContaNova(),
                        new RegraValorAlto()));
        LocalDateTime dataHora = LocalDateTime.of(2026, 9, 8, 3, 0);
        ContextoAnalise contexto = new ContextoAnalise(evento(
                new BigDecimal("6000"), "USA", dataHora, dataHora.minusDays(10)));

        cadeia.analisar(contexto);

        assertThat(contexto.getPontuacao()).isEqualTo(155);
        assertThat(contexto.getRegrasDisparadas()).containsExactly(
                "VALOR_ALTO",
                "CONTA_NOVA",
                "HORARIO_SUSPEITO",
                "VALOR_MUITO_ALTO_CONTA_NOVA",
                "PAIS_ESTRANGEIRO");
        assertThat(NivelRisco.classificar(contexto.getPontuacao())).isEqualTo(NivelRisco.BLOQUEADA);
    }
}
