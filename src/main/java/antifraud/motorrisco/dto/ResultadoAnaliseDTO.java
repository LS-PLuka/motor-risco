package antifraud.motorrisco.dto;

import antifraud.motorrisco.enums.NivelRisco;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ResultadoAnaliseDTO(
        UUID transacaoId,
        int pontuacao,
        NivelRisco nivel,
        List<String> regrasDisparadas,
        LocalDateTime analisadoEm
) {
    public ResultadoAnaliseDTO {
        regrasDisparadas = List.copyOf(regrasDisparadas);
    }
}
