package com.artarkatesoft.learnkafka.libraryeventsconsumer.config;

import com.artarkatesoft.learnkafka.libraryeventsconsumer.services.LibraryEventsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import javax.persistence.EntityNotFoundException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@EnableKafka
public class LibraryEventsConsumerConfig {

    @Autowired
    LibraryEventsService libraryEventsService;

    @Bean
    ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ObjectProvider<ConsumerFactory<Object, Object>> kafkaConsumerFactory,
            KafkaProperties properties) {

        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, kafkaConsumerFactory
                .getIfAvailable(() -> new DefaultKafkaConsumerFactory<>(properties.buildConsumerProperties())));

//        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
//        factory.setConcurrency(3);

        factory.setErrorHandler((thrownException, data) ->
                log.error("Exception in consumerConfig is {} and teh record is {}", thrownException.getMessage(), data));

        factory.setRetryTemplate(retryTemplate());
        factory.setRecoveryCallback(context -> {
            if (context.getLastThrowable().getCause() instanceof RecoverableDataAccessException) {
                //invoke recovery logic
                log.info("Inside recoverable logic");
/*
                Stream.of(context.attributeNames())
                        .forEach(attributeName ->
                                log.info("Attribute name is: {}, value is: {}", attributeName, context.getAttribute(attributeName)));
*/
                ConsumerRecord<Integer, String> consumerRecord = (ConsumerRecord<Integer, String>) context.getAttribute("record");
                libraryEventsService.handleRecovery(consumerRecord);

            } else {
                log.info("Inside Non-recoverable logic");
                throw new RuntimeException(context.getLastThrowable().getMessage());
            }
            return null;
        });
        return factory;
    }

    private RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(retryPolicy());
        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(1000);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        return retryTemplate;
    }

    private RetryPolicy retryPolicy() {
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(IllegalArgumentException.class, false);
        retryableExceptions.put(EntityNotFoundException.class, false);
        retryableExceptions.put(RecoverableDataAccessException.class, true);
        return new SimpleRetryPolicy(3, retryableExceptions, true);
    }
}
