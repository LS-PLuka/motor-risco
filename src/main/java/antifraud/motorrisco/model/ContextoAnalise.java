package antifraud.motorrisco.model;

import antifraud.motorrisco.dto.TransacaoEventoDTO;
import java.util.ArrayList;
import java.util.List;

public class ContextoAnalise {

    private final TransacaoEventoDTO transacao;
    private int pontuacao;
    private final List<String> regrasDisparadas = new ArrayList<>();

    public ContextoAnalise(TransacaoEventoDTO transacao) {
        this.transacao = transacao;
    }

    public void registrarRegraDisparada(int pontos, String regra) {
        this.pontuacao += pontos;
        this.regrasDisparadas.add(regra);
    }

    public TransacaoEventoDTO getTransacao() {
        return transacao;
    }

    public int getPontuacao() {
        return pontuacao;
    }

    public List<String> getRegrasDisparadas() {
        return List.copyOf(regrasDisparadas);
    }
}
