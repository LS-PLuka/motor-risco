package antifraud.motorrisco.regras;

import antifraud.motorrisco.model.ContextoAnalise;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static antifraud.motorrisco.EventoFixture.evento;
import static org.assertj.core.api.Assertions.assertThat;

class RegraPaisEstrangeiroTest {

    private final RegraPaisEstrangeiro regra = new RegraPaisEstrangeiro();

    @ParameterizedTest(name = "país={0}, dispara={1}")
    @CsvSource({
            "BRA, false",
            "USA, true"
    })
    @DisplayName("Regra dispara somente para código de país diferente de BRA")
    void analisar_codigoPais_deveIdentificarPaisEstrangeiro(String codigoPais, boolean dispara) {
        LocalDateTime dataHora = LocalDateTime.of(2026, 9, 8, 14, 30);
        ContextoAnalise contexto = new ContextoAnalise(evento(
                new BigDecimal("100"), codigoPais, dataHora, dataHora.minusDays(60)));

        regra.analisar(contexto);

        if (dispara) {
            assertThat(contexto.getPontuacao()).isEqualTo(25);
            assertThat(contexto.getRegrasDisparadas()).containsExactly(RegraPaisEstrangeiro.NOME);
        } else {
            assertThat(contexto.getPontuacao()).isZero();
            assertThat(contexto.getRegrasDisparadas()).isEmpty();
        }
    }
}
