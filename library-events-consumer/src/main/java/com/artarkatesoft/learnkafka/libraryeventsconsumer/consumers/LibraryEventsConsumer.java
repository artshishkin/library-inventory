package com.artarkatesoft.learnkafka.libraryeventsconsumer.consumers;

import com.artarkatesoft.learnkafka.libraryeventsconsumer.services.LibraryEventsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LibraryEventsConsumer {

    static final String TOPIC = "library-events";

    private final LibraryEventsService service;

    @KafkaListener(topics = {TOPIC})
    public void onMessage(ConsumerRecord<Integer, String> consumerRecord) throws JsonProcessingException {
        log.info("Consumer record: {}", consumerRecord);
        service.processLibraryEvents(consumerRecord);
    }
}
