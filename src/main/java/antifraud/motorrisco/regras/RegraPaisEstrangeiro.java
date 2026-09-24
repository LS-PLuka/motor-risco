package antifraud.motorrisco.regras;

import antifraud.motorrisco.model.ContextoAnalise;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(50)
@Component
public class RegraPaisEstrangeiro extends RegraRisco {

    static final String NOME = "PAIS_ESTRANGEIRO";
    private static final String PAIS_BRASIL = "BRA";
    private static final int PONTOS = 25;

    @Override
    protected void aplicarRegra(ContextoAnalise contexto) {
        if (!PAIS_BRASIL.equals(contexto.getTransacao().codigoPais())) {
            contexto.registrarRegraDisparada(PONTOS, NOME);
        }
    }
}
