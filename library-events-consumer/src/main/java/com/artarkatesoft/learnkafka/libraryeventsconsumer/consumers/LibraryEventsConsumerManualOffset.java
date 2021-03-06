package com.artarkatesoft.learnkafka.libraryeventsconsumer.consumers;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.support.Acknowledgment;

@Slf4j
//@Component
public class LibraryEventsConsumerManualOffset implements AcknowledgingMessageListener<Integer,String> {

    static final String TOPIC = "library-events";

    @KafkaListener(topics = {TOPIC})
    @Override
    public void onMessage(ConsumerRecord<Integer, String> consumerRecord, Acknowledgment acknowledgment) {
        log.info("Consumer record: {}", consumerRecord);
        acknowledgment.acknowledge();
    }
}
