package antifraud.motorrisco.regras;

import antifraud.motorrisco.model.ContextoAnalise;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static antifraud.motorrisco.EventoFixture.evento;
import static org.assertj.core.api.Assertions.assertThat;

class RegraValorMuitoAltoContaNovaTest {

    private static final LocalDateTime DATA_HORA = LocalDateTime.of(2026, 9, 8, 14, 30);
    private final RegraValorMuitoAltoContaNova regra = new RegraValorMuitoAltoContaNova();

    @ParameterizedTest(name = "idade={0} dias, valor={1}, dispara={2}")
    @CsvSource({
            "29, 5000.01, true",
            "30, 5000.01, false",
            "31, 5000.01, false",
            "29, 5000.00, false",
            "29, 6000.00, true",
            "30, 5000.00, false"
    })
    @DisplayName("Regra exige simultaneamente conta nova e valor muito alto")
    void analisar_idadeEValor_deveRespeitarLimites(long idadeDias, String valor, boolean dispara) {
        ContextoAnalise contexto = new ContextoAnalise(evento(
                new BigDecimal(valor), DATA_HORA, DATA_HORA.minusDays(idadeDias)));

        regra.analisar(contexto);

        if (dispara) {
            assertThat(contexto.getPontuacao()).isEqualTo(45);
            assertThat(contexto.getRegrasDisparadas()).containsExactly(RegraValorMuitoAltoContaNova.NOME);
        } else {
            assertThat(contexto.getPontuacao()).isZero();
            assertThat(contexto.getRegrasDisparadas()).isEmpty();
        }
    }
}
