package com.healthcare.patient.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
/**
 * This class is a configuration class for RabbitMQ.
 * It provides a bean for TopicExchange, Jackson2JsonMessageConverter, and RabbitTemplate.
 */
public class RabbitMqConfig {

    /**
     * Bean for TopicExchange.
     * It creates a TopicExchange bean with the name specified in the application.properties file.
     *
     * @param exchangeName name of the exchange specified in the application.properties file
     * @return TopicExchange bean
     */
    @Bean
    public TopicExchange userEventsExchange(
            @Value("${healthcare.user-events.exchange}") String exchangeName
    ) {
        return new TopicExchange(exchangeName, true, false);
    }

    /**
     * Bean for Jackson2JsonMessageConverter.
     * It creates a Jackson2JsonMessageConverter bean.
     *
     * @return Jackson2JsonMessageConverter bean
     */
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Bean for RabbitTemplate.
     * It creates a RabbitTemplate bean with the specified ConnectionFactory and Jackson2JsonMessageConverter.
     *
     * @param connectionFactory ConnectionFactory bean
     * @param messageConverter  Jackson2JsonMessageConverter bean
     * @return RabbitTemplate bean
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
