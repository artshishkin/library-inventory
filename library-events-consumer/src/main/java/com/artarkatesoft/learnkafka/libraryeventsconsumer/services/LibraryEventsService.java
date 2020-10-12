package com.artarkatesoft.learnkafka.libraryeventsconsumer.services;

import com.artarkatesoft.learnkafka.libraryeventsconsumer.entity.LibraryEvent;
import com.artarkatesoft.learnkafka.libraryeventsconsumer.entity.LibraryEventType;
import com.artarkatesoft.learnkafka.libraryeventsconsumer.repositories.LibraryEventsRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

import javax.persistence.EntityNotFoundException;

@Service
@Slf4j
@RequiredArgsConstructor
public class LibraryEventsService {

    private final LibraryEventsRepository repository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<Integer, String> kafkaTemplate;


    public void processLibraryEvents(ConsumerRecord<Integer, String> consumerRecord) throws JsonProcessingException {
        LibraryEvent libraryEvent = objectMapper.readValue(consumerRecord.value(), LibraryEvent.class);
        log.info("Library Event: {}", libraryEvent);
        LibraryEventType libraryEventType = libraryEvent.getLibraryEventType();

        //just for study purposes
        if (libraryEvent.getLibraryEventId() != null && libraryEvent.getLibraryEventId() == 0) {
            throw new RecoverableDataAccessException("Temporary Network Issue");
        }

        switch (libraryEventType) {
            case NEW:
                libraryEvent.setLibraryEventId(null);
                save(libraryEvent);
                break;
            case UPDATE:
                validate(libraryEvent);
                save(libraryEvent);
                break;
            default:
                log.info("Wrong Library Event Type: `{}` for LibraryEvent {}", libraryEventType, libraryEvent);
        }

    }

    private void validate(LibraryEvent libraryEvent) {
        Integer libraryEventId = libraryEvent.getLibraryEventId();
        if (libraryEventId == null)
            throw new IllegalArgumentException("Library Id is missing for " + libraryEvent);
        if (!repository.existsById(libraryEventId))
            throw new EntityNotFoundException("Library Event with ID `" + libraryEventId + "` not found");
        log.info("Validation is successful for the library event: {}", libraryEvent);
    }

    private void save(LibraryEvent libraryEvent) {
        libraryEvent.getBook().setLibraryEvent(libraryEvent);
        LibraryEvent savedLibraryEvent = repository.save(libraryEvent);
        log.info("Successfully saved Library Event {}", savedLibraryEvent);
    }

    public void handleRecovery(ConsumerRecord<Integer, String> consumerRecord) {
        Integer key = consumerRecord.key();
        String message = consumerRecord.value();
        ListenableFuture<SendResult<Integer, String>> future = kafkaTemplate.sendDefault(key, message);
        future.addCallback(
                result -> handleSuccess(key, message, result),
                ex -> handleFailure(key, message, ex)
        );
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
