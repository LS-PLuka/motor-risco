package antifraud.motorrisco.regras;

import antifraud.motorrisco.model.ContextoAnalise;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import static antifraud.motorrisco.EventoFixture.evento;
import static org.assertj.core.api.Assertions.assertThat;

class RegraRiscoTest {

    @Test
    @DisplayName("Cadeia encaminha o mesmo contexto e acumula todas as contribuições")
    void analisar_multiplasRegras_deveEncaminharMesmoContextoEAcumularPontos() {
        AtomicReference<ContextoAnalise> contextoRecebido = new AtomicReference<>();
        RegraRisco primeira = regraQueAdiciona(10, "PRIMEIRA", null);
        RegraRisco segunda = regraQueAdiciona(20, "SEGUNDA", contextoRecebido);
        primeira.definirProxima(segunda);
        LocalDateTime dataHora = LocalDateTime.of(2026, 9, 8, 14, 30);
        ContextoAnalise contexto = new ContextoAnalise(
                evento(BigDecimal.ONE, dataHora, dataHora.minusDays(60)));

        primeira.analisar(contexto);

        assertThat(contextoRecebido.get()).isSameAs(contexto);
        assertThat(contexto.getPontuacao()).isEqualTo(30);
        assertThat(contexto.getRegrasDisparadas()).containsExactly("PRIMEIRA", "SEGUNDA");
    }

    private RegraRisco regraQueAdiciona(
            int pontos,
            String nome,
            AtomicReference<ContextoAnalise> contextoRecebido
    ) {
        return new RegraRisco() {
            @Override
            protected void aplicarRegra(ContextoAnalise contexto) {
                if (contextoRecebido != null) {
                    contextoRecebido.set(contexto);
                }
                contexto.registrarRegraDisparada(pontos, nome);
            }
        };
    }
}
