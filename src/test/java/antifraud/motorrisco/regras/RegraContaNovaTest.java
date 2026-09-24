package antifraud.motorrisco.regras;

import antifraud.motorrisco.model.ContextoAnalise;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static antifraud.motorrisco.EventoFixture.evento;
import static org.assertj.core.api.Assertions.assertThat;

class RegraContaNovaTest {

    private static final LocalDateTime DATA_HORA = LocalDateTime.of(2026, 9, 8, 14, 30);
    private final RegraContaNova regra = new RegraContaNova();

    @ParameterizedTest(name = "idade={0} dias, valor={1}, dispara={2}")
    @CsvSource({
            "29, 1000.01, true",
            "30, 1000.01, false",
            "31, 1000.01, false",
            "29, 999.99, false",
            "29, 1000, false",
            "30, 999.99, false"
    })
    @DisplayName("Regra respeita os limites de idade e valor")
    void analisar_idadeEValor_deveRespeitarLimites(long idadeDias, String valor, boolean dispara) {
        ContextoAnalise contexto = new ContextoAnalise(evento(
                new BigDecimal(valor), DATA_HORA, DATA_HORA.minusDays(idadeDias)));

        regra.analisar(contexto);

        if (dispara) {
            assertThat(contexto.getPontuacao()).isEqualTo(35);
            assertThat(contexto.getRegrasDisparadas()).containsExactly(RegraContaNova.NOME);
        } else {
            assertThat(contexto.getPontuacao()).isZero();
            assertThat(contexto.getRegrasDisparadas()).isEmpty();
        }
    }
}
