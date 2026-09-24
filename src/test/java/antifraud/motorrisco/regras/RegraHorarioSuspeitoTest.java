package antifraud.motorrisco.regras;

import antifraud.motorrisco.model.ContextoAnalise;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static antifraud.motorrisco.EventoFixture.evento;
import static org.assertj.core.api.Assertions.assertThat;

class RegraHorarioSuspeitoTest {

    private final RegraHorarioSuspeito regra = new RegraHorarioSuspeito();

    @ParameterizedTest(name = "horário={0}, valor={1}, dispara={2}")
    @CsvSource({
            "23:59:59, 500.01, false",
            "00:00:00, 500.01, true",
            "03:00:00, 500.01, true",
            "05:59:59, 500.01, true",
            "06:00:00, 500.01, false",
            "03:00:00, 500.00, false",
            "03:00:00, 500.01, true",
            "14:00:00, 500.01, false"
    })
    @DisplayName("Regra respeita as fronteiras de horário e valor")
    void analisar_horarioEValor_deveRespeitarLimites(String horario, String valor, boolean dispara) {
        LocalDateTime dataHora = LocalDateTime.parse("2026-09-08T" + horario);
        ContextoAnalise contexto = new ContextoAnalise(evento(
                new BigDecimal(valor), dataHora, dataHora.minusDays(60)));

        regra.analisar(contexto);

        if (dispara) {
            assertThat(contexto.getPontuacao()).isEqualTo(20);
            assertThat(contexto.getRegrasDisparadas()).containsExactly(RegraHorarioSuspeito.NOME);
        } else {
            assertThat(contexto.getPontuacao()).isZero();
            assertThat(contexto.getRegrasDisparadas()).isEmpty();
        }
    }
}
