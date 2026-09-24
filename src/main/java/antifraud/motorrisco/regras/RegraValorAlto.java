package antifraud.motorrisco.regras;

import antifraud.motorrisco.model.ContextoAnalise;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Order(10)
@Component
public class RegraValorAlto extends RegraRisco {

    static final String NOME = "VALOR_ALTO";
    private static final BigDecimal LIMITE = new BigDecimal("5000");
    private static final int PONTOS = 30;

    @Override
    protected void aplicarRegra(ContextoAnalise contexto) {
        if (contexto.getTransacao().valor().compareTo(LIMITE) > 0) {
            contexto.registrarRegraDisparada(PONTOS, NOME);
        }
    }
}
