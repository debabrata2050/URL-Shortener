package com.deb.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_CLICK_EVENTS = "click.events.exchange";
    public static final String QUEUE_CLICK_EVENTS = "click.events.queue";
    public static final String ROUTING_KEY_CLICK = "click.redirect";

    public static final String DLX_CLICK_EVENTS = "click.events.dlx";
    public static final String DLQ_CLICK_EVENTS = "click.events.dlq";
    public static final String DLQ_ROUTING_KEY = "click.events.dead";

    @Bean
    public TopicExchange clickEventsExchange() {
        return new TopicExchange(EXCHANGE_CLICK_EVENTS, true, false);
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(DLX_CLICK_EVENTS, true, false);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_CLICK_EVENTS).build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(DLQ_ROUTING_KEY);
    }

    @Bean
    public Queue clickEventsQueue() {
        return QueueBuilder.durable(QUEUE_CLICK_EVENTS)
                .withArgument("x-dead-letter-exchange", DLX_CLICK_EVENTS)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding clickEventsBinding() {
        return BindingBuilder.bind(clickEventsQueue())
                .to(clickEventsExchange())
                .with(ROUTING_KEY_CLICK);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
