package com.artarkatesoft.learnkafka.libraryeventsproducer.producers;

import com.artarkatesoft.learnkafka.libraryeventsproducer.domain.LibraryEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@Slf4j
@RequiredArgsConstructor
public class LibraryEventProducer {

    private final KafkaTemplate<Integer, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    public static final String TOPIC = "library-events";

    public void sendLibraryEvent(LibraryEvent libraryEvent) throws JsonProcessingException {
        Integer key = libraryEvent.getLibraryEventId();
        String jsonValue = objectMapper.writeValueAsString(libraryEvent);
        ListenableFuture<SendResult<Integer, String>> future = kafkaTemplate.sendDefault(key, jsonValue);
        future.addCallback(
                result -> handleSuccess(key, jsonValue, result),
                ex -> handleFailure(key, jsonValue, ex)
        );
    }

    public SendResult<Integer, String> sendLibraryEventSynchronous(LibraryEvent libraryEvent) throws JsonProcessingException, ExecutionException, InterruptedException, TimeoutException {
        Integer key = libraryEvent.getLibraryEventId();
        String jsonValue = objectMapper.writeValueAsString(libraryEvent);
        ListenableFuture<SendResult<Integer, String>> future = kafkaTemplate.sendDefault(key, jsonValue);
//        future.addCallback(
//                result -> handleSuccess(key, jsonValue, result),
//                ex -> handleFailure(key, jsonValue, ex)
//        );

        try {
            return future.get(1, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            handleFailure(key, jsonValue, e);
            throw e;
        }
    }

    public ListenableFuture<SendResult<Integer, String>> sendLibraryEventUsingProducerRecord(LibraryEvent libraryEvent) throws JsonProcessingException {
        Integer key = libraryEvent.getLibraryEventId();
        String jsonValue = objectMapper.writeValueAsString(libraryEvent);

        ListenableFuture<SendResult<Integer, String>> future = kafkaTemplate.send(generateProducerRecord(key, jsonValue));
        future.addCallback(
                result -> handleSuccess(key, jsonValue, result),
                ex -> handleFailure(key, jsonValue, ex)
        );
        return future;
    }

    private ProducerRecord<Integer, String> generateProducerRecord(Integer key, String jsonValue) {
        Iterable<Header> headers = List.of(new RecordHeader("event-source", "scanner".getBytes()));
        return new ProducerRecord<>(TOPIC, null, key, jsonValue, headers);
    }

    private void handleFailure(Integer key, String value, Throwable ex) {
        log.error("Exception while sending message the key: {} and the value is {}", key, value, ex);
    }

    private void handleSuccess(Integer key, String value, SendResult<Integer, String> result) {

        Integer partition = (result != null && result.getRecordMetadata() != null) ?
                result.getRecordMetadata().partition() : null;

        log.info("Message sent successfully for the key: {} and the value is {}, partition is {}", key, value, partition);
    }

}
