package com.qiraht.spring_lms.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "progress.exchange";
    public static final String QUEUE = "progress.report.export.q";
    public static final String DLQ = "progress.report.export.dlq";
    public static final String ROUTING_KEY = "progress.report.export";
    public static final String DLQ_ROUTING_KEY = "progress.report.export.dlq";

    @Bean
    public DirectExchange progressExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public Queue progressReportQueue() {
        return QueueBuilder.durable(QUEUE)
                .withArgument("x-dead-letter-exchange", EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue progressReportDlq() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public Binding progressReportBinding(DirectExchange progressExchange, Queue progressReportQueue) {
        return BindingBuilder.bind(progressReportQueue).to(progressExchange).with(ROUTING_KEY);
    }

    @Bean
    public Binding progressReportDlqBinding(DirectExchange progressExchange, Queue progressReportDlq) {
        return BindingBuilder.bind(progressReportDlq).to(progressExchange).with(DLQ_ROUTING_KEY);
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public JacksonJsonMessageConverter jacksonJsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory, JacksonJsonMessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }
}
