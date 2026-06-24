package com.example.lms.shared.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DomainEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public DomainEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishAfterCommit(String topic, String key, Object event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publish(topic, key, event);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publish(topic, key, event);
            }
        });
    }

    private void publish(String topic, String key, Object event) {
        try {
            kafkaTemplate.send(topic, key, event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish event to topic={} key={}", topic, key, ex);
                            return;
                        }
                        log.info("Published event topic={} key={} type={}", topic, key, event.getClass().getSimpleName());
                    });
        } catch (RuntimeException ex) {
            log.error("Failed to start event publish topic={} key={} type={}",
                    topic,
                    key,
                    event.getClass().getSimpleName(),
                    ex);
        }
    }
}
