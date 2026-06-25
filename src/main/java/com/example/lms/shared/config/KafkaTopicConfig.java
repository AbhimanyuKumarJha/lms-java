package com.example.lms.shared.config;

import com.example.lms.shared.events.EventNames;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    NewTopic userCreatedTopic() {
        return topic(EventNames.USER_CREATED);
    }

    @Bean
    NewTopic bookBorrowedTopic() {
        return topic(EventNames.BOOK_BORROWED);
    }

    @Bean
    NewTopic bookReturnedTopic() {
        return topic(EventNames.BOOK_RETURNED);
    }

    @Bean
    NewTopic fineGeneratedTopic() {
        return topic(EventNames.FINE_GENERATED);
    }

    @Bean
    NewTopic signupOtpRequestedTopic() {
        return topic(EventNames.SIGNUP_OTP_REQUESTED);
    }

    @Bean
    NewTopic userVerifiedTopic() {
        return topic(EventNames.USER_VERIFIED);
    }

    private NewTopic topic(String name) {
        return TopicBuilder.name(name)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
