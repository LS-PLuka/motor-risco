package antifraud.motorrisco;

import antifraud.motorrisco.dto.TransacaoEventoDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public final class EventoFixture {

    private EventoFixture() {
    }

    public static TransacaoEventoDTO evento(BigDecimal valor, LocalDateTime dataHora, LocalDateTime contaCriadaEm) {
        return new TransacaoEventoDTO(
                UUID.fromString("7f3e4c2a-1b5d-4e8f-9a2c-3d6b7e1f4a8c"),
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                valor,
                "RESTAURANTE",
                "BRA",
                dataHora,
                contaCriadaEm
        );
    }
}
