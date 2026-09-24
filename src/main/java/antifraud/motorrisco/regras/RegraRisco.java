package antifraud.motorrisco.regras;

import antifraud.motorrisco.model.ContextoAnalise;

public abstract class RegraRisco {

    private RegraRisco proximaRegra;

    public RegraRisco definirProxima(RegraRisco proximaRegra) {
        this.proximaRegra = proximaRegra;
        return proximaRegra;
    }

    public void analisar(ContextoAnalise contexto) {
        aplicarRegra(contexto);

        if (proximaRegra != null) {
            proximaRegra.analisar(contexto);
        }
    }

    protected abstract void aplicarRegra(ContextoAnalise contexto);
}
