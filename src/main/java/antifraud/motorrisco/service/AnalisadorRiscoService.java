package antifraud.motorrisco.service;

import antifraud.motorrisco.dto.ResultadoAnaliseDTO;
import antifraud.motorrisco.dto.TransacaoEventoDTO;
import antifraud.motorrisco.enums.NivelRisco;
import antifraud.motorrisco.model.ContextoAnalise;
import antifraud.motorrisco.publisher.ResultadoAnalisePublisher;
import antifraud.motorrisco.regras.RegraRisco;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static antifraud.motorrisco.config.CadeiaRiscoConfig.CADEIA_RISCO;

@Service
@RequiredArgsConstructor
public class AnalisadorRiscoService {

    @Qualifier(CADEIA_RISCO)
    private final RegraRisco cadeiaRisco;
    private final ResultadoAnalisePublisher resultadoAnalisePublisher;

    public ResultadoAnaliseDTO analisar(TransacaoEventoDTO transacao) {
        ContextoAnalise contexto = new ContextoAnalise(transacao);
        cadeiaRisco.analisar(contexto);

        ResultadoAnaliseDTO resultado = new ResultadoAnaliseDTO(
                transacao.transacaoId(),
                contexto.getPontuacao(),
                NivelRisco.classificar(contexto.getPontuacao()),
                contexto.getRegrasDisparadas(),
                LocalDateTime.now()
        );
        resultadoAnalisePublisher.publicar(resultado);
        return resultado;
    }
}
