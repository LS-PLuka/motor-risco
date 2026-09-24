package antifraud.motorrisco.enums;

public enum NivelRisco {
    APROVADA,
    SINALIZADA,
    BLOQUEADA;

    public static NivelRisco classificar(int pontuacao) {
        if (pontuacao >= 70) {
            return BLOQUEADA;
        }

        if (pontuacao >= 40) {
            return SINALIZADA;
        }

        return APROVADA;
    }
}
