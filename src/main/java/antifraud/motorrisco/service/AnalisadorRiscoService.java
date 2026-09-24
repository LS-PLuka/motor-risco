package antifraud.motorrisco.service;

import antifraud.motorrisco.dto.ResultadoAnaliseDTO;
import antifraud.motorrisco.dto.TransacaoEventoDTO;
import antifraud.motorrisco.enums.NivelRisco;
import antifraud.motorrisco.model.ContextoAnalise;
import antifraud.motorrisco.regras.RegraRisco;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import static antifraud.motorrisco.config.CadeiaRiscoConfig.CADEIA_RISCO;

@Service
@RequiredArgsConstructor
public class AnalisadorRiscoService {

    @Qualifier(CADEIA_RISCO)
    private final RegraRisco cadeiaRisco;

    public ResultadoAnaliseDTO analisar(TransacaoEventoDTO transacao) {
        ContextoAnalise contexto = new ContextoAnalise(transacao);
        cadeiaRisco.analisar(contexto);

        return new ResultadoAnaliseDTO(
                transacao.transacaoId(),
                transacao.contaId(),
                contexto.getPontuacao(),
                NivelRisco.classificar(contexto.getPontuacao()),
                contexto.getRegrasDisparadas()
        );
    }
}
