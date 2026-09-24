package antifraud.motorrisco.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransacaoEventoDTO(
        UUID transacaoId,
        UUID contaId,
        BigDecimal valor,
        String categoria,
        String codigoPais,
        LocalDateTime dataHora
) { }
