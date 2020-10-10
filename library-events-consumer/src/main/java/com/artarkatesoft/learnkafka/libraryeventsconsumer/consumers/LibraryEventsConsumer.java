package com.artarkatesoft.learnkafka.libraryeventsconsumer.consumers;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LibraryEventsConsumer {

    static final String TOPIC = "library-events";

    @KafkaListener(topics = {TOPIC})
    public void onMessage(ConsumerRecord<Integer, String> consumerRecord) {
        log.info("Consumer record: {}", consumerRecord);
    }
}
