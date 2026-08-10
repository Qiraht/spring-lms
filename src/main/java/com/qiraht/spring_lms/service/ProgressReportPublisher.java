package com.qiraht.spring_lms.service;

import com.qiraht.spring_lms.config.RabbitMQConfig;
import com.qiraht.spring_lms.dto.ProgressReportMessage;
import com.qiraht.spring_lms.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressReportPublisher {

    private final RabbitTemplate rabbitTemplate;

    @CircuitBreaker(name = "rabbitmq-publish", fallbackMethod = "publishFallback")
    public void publish(ProgressReportMessage message) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, message);
        log.info("Queued progress report export for class {} to {}", message.getClassId(), message.getRecipientEmail());
    }

    private void publishFallback(ProgressReportMessage message, Throwable throwable) {
        log.error(
                "Failed to publish progress report export for class {}: {}",
                message.getClassId(),
                throwable.getMessage());
        throw new ServiceUnavailableException("Progress report export is temporarily unavailable");
    }
}
