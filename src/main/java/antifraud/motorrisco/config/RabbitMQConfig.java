package antifraud.motorrisco.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_TRANSACOES = "transacoes.exchange";
    public static final String FILA_ANALISE = "transacoes.analise";
    public static final String ROUTING_KEY_RISCO = "transacoes.risco";
    public static final String EXCHANGE_RISCO = "risco.exchange";
    public static final String FILA_RESULTADOS = "risco.resultados";
    public static final String ROUTING_KEY_RESULTADO = "risco.resultado";

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
    public DirectExchange exchangeRisco() {
        return new DirectExchange(EXCHANGE_RISCO);
    }

    @Bean
    public Queue filaResultados() {
        return new Queue(FILA_RESULTADOS, true);
    }

    @Bean
    public Binding bindingResultados(
            Queue filaResultados,
            DirectExchange exchangeRisco
    ) {
        return BindingBuilder
                .bind(filaResultados)
                .to(exchangeRisco)
                .with(ROUTING_KEY_RESULTADO);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        converter.setJavaTypeMapper(typeMapper);
        converter.setAlwaysConvertToInferredType(true);
        return converter;
    }
}
