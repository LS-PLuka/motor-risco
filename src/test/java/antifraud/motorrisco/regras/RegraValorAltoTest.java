package antifraud.motorrisco.regras;

import antifraud.motorrisco.model.ContextoAnalise;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static antifraud.motorrisco.EventoFixture.evento;
import static org.assertj.core.api.Assertions.assertThat;

class RegraValorAltoTest {

    private RegraValorAlto regra;
    private LocalDateTime dataHora;

    @BeforeEach
    void setUp() {
        regra = new RegraValorAlto();
        dataHora = LocalDateTime.of(2026, 9, 8, 14, 30);
    }

    @Test
    @DisplayName("Valor abaixo do limite não altera o contexto")
    void analisar_valorAbaixoDoLimite_naoDeveAdicionarPontuacao() {
        ContextoAnalise contexto = contextoComValor("4999.99");

        regra.analisar(contexto);

        assertThat(contexto.getPontuacao()).isZero();
        assertThat(contexto.getRegrasDisparadas()).isEmpty();
    }

    @Test
    @DisplayName("Valor igual ao limite não altera o contexto")
    void analisar_valorIgualAoLimite_naoDeveAdicionarPontuacao() {
        ContextoAnalise contexto = contextoComValor("5000");

        regra.analisar(contexto);

        assertThat(contexto.getPontuacao()).isZero();
        assertThat(contexto.getRegrasDisparadas()).isEmpty();
    }

    @Test
    @DisplayName("Valor acima do limite adiciona pontos e registra a regra")
    void analisar_valorAcimaDoLimite_deveAdicionarPontuacao() {
        ContextoAnalise contexto = contextoComValor("5000.01");

        regra.analisar(contexto);

        assertThat(contexto.getPontuacao()).isEqualTo(30);
        assertThat(contexto.getRegrasDisparadas()).containsExactly(RegraValorAlto.NOME);
    }

    private ContextoAnalise contextoComValor(String valor) {
        return new ContextoAnalise(evento(new BigDecimal(valor), dataHora, dataHora.minusDays(60)));
    }
}
