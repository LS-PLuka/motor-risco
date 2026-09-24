package antifraud.motorrisco.model;

import antifraud.motorrisco.dto.TransacaoEventoDTO;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ContextoAnalise {

    private final TransacaoEventoDTO transacao;
    private int pontuacao;
    private final List<String> regrasDisparadas = new ArrayList<>();

    public ContextoAnalise(TransacaoEventoDTO transacao) {
        this.transacao = transacao;
    }

    public void adicionarPontuacao(int pontos, String regra) {
        this.pontuacao += pontos;
        this.regrasDisparadas.add(regra);
    }
}
