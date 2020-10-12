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
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;

@Service
@Slf4j
@RequiredArgsConstructor
public class LibraryEventsService {

    private final LibraryEventsRepository repository;
    private final ObjectMapper objectMapper;


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
}
