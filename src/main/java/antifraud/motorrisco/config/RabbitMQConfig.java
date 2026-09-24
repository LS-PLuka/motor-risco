package antifraud.motorrisico.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_TRANSACOES = "transacoes.exchange";
    public static final String FILA_ANALISE = "transacoes.analise";
    public static final String ROUTING_KEY_RISCO = "transacoes.risco";

    @Bean
    public DirectExchange exchangeTransacoes() {
        return new DirectExchange(EXCHANGE_TRANSACOES);
    }

    @Bean
    public Queue filaAnalise() {
        return new Queue(FILA_ANALISE, true);
    }

    @Bean
    public Binding bindingAnalise(
            Queue filaAnalise,
            DirectExchange exchangeTransacoes
    ) {
        return BindingBuilder
                .bind(filaAnalise)
                .to(exchangeTransacoes)
                .with(ROUTING_KEY_RISCO);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
