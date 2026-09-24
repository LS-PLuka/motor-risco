package antifraud.motorrisco.regras;

import antifraud.motorrisco.dto.TransacaoEventoDTO;
import antifraud.motorrisco.model.ContextoAnalise;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;

@Order(40)
@Component
public class RegraValorMuitoAltoContaNova extends RegraRisco {

    static final String NOME = "VALOR_MUITO_ALTO_CONTA_NOVA";
    private static final BigDecimal LIMITE_VALOR = new BigDecimal("5000");
    private static final Duration LIMITE_IDADE = Duration.ofDays(30);
    private static final int PONTOS = 45;

    @Override
    protected void aplicarRegra(ContextoAnalise contexto) {
        TransacaoEventoDTO transacao = contexto.getTransacao();
        Duration idadeConta = Duration.between(transacao.contaCriadaEm(), transacao.dataHora());

        if (!idadeConta.isNegative()
                && idadeConta.compareTo(LIMITE_IDADE) < 0
                && transacao.valor().compareTo(LIMITE_VALOR) > 0) {
            contexto.registrarRegraDisparada(PONTOS, NOME);
        }
    }
}
