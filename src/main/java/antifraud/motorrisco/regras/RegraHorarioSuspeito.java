package antifraud.motorrisco.regras;

import antifraud.motorrisco.dto.TransacaoEventoDTO;
import antifraud.motorrisco.model.ContextoAnalise;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalTime;

@Order(30)
@Component
public class RegraHorarioSuspeito extends RegraRisco {

    static final String NOME = "HORARIO_SUSPEITO";
    private static final LocalTime FIM_JANELA = LocalTime.of(6, 0);
    private static final BigDecimal LIMITE_VALOR = new BigDecimal("500");
    private static final int PONTOS = 20;

    @Override
    protected void aplicarRegra(ContextoAnalise contexto) {
        TransacaoEventoDTO transacao = contexto.getTransacao();
        LocalTime horario = transacao.dataHora().toLocalTime();

        if (horario.isBefore(FIM_JANELA)
                && transacao.valor().compareTo(LIMITE_VALOR) > 0) {
            contexto.registrarRegraDisparada(PONTOS, NOME);
        }
    }
}
