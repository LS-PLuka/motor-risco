package antifraud.motorrisco.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class NivelRiscoTest {

    @ParameterizedTest(name = "score {0} deve resultar em {1}")
    @CsvSource({
            "0, APROVADA",
            "39, APROVADA",
            "40, SINALIZADA",
            "69, SINALIZADA",
            "70, BLOQUEADA",
            "100, BLOQUEADA"
    })
    @DisplayName("Classificação respeita todos os limites de score")
    void classificar_pontuacao_deveRespeitarLimites(int pontuacao, NivelRisco esperado) {
        assertThat(NivelRisco.classificar(pontuacao)).isEqualTo(esperado);
    }
}
