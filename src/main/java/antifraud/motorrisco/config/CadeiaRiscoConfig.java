package antifraud.motorrisco.config;

import antifraud.motorrisco.regras.RegraRisco;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class CadeiaRiscoConfig {

    public static final String CADEIA_RISCO = "cadeiaRisco";

    @Bean(CADEIA_RISCO)
    @Primary
    public RegraRisco cadeiaRisco(List<RegraRisco> regras) {
        List<RegraRisco> regrasOrdenadas = new ArrayList<>(regras);
        AnnotationAwareOrderComparator.sort(regrasOrdenadas);

        for (int indice = 0; indice < regrasOrdenadas.size() - 1; indice++) {
            regrasOrdenadas.get(indice).definirProxima(regrasOrdenadas.get(indice + 1));
        }

        return regrasOrdenadas.getFirst();
    }
}
