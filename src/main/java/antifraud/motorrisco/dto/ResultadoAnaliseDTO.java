package antifraud.motorrisco.dto;

import antifraud.motorrisco.enums.NivelRisco;

import java.util.List;
import java.util.UUID;

public record ResultadoAnaliseDTO(
        UUID transacaoId,
        UUID contaId,
        int pontuacao,
        NivelRisco classificacao,
        List<String> regrasDisparadas
) {
    public ResultadoAnaliseDTO {
        regrasDisparadas = List.copyOf(regrasDisparadas);
    }
}
