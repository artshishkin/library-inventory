package com.artarkatesoft.learnkafka.libraryeventsconsumer.services;

import com.artarkatesoft.learnkafka.libraryeventsconsumer.entity.LibraryEvent;
import com.artarkatesoft.learnkafka.libraryeventsconsumer.entity.LibraryEventType;
import com.artarkatesoft.learnkafka.libraryeventsconsumer.repositories.LibraryEventsRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;

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
        switch (libraryEventType) {
            case NEW:
                libraryEvent.setLibraryEventId(null);
                save(libraryEvent);
                break;
            case UPDATE:
                break;
            default:
                log.info("Wrong Library Event Type: `{}` for LibraryEvent {}", libraryEventType, libraryEvent);
        }

    }

    private void save(LibraryEvent libraryEvent) {
        libraryEvent.getBook().setLibraryEvent(libraryEvent);
        LibraryEvent savedLibraryEvent = repository.save(libraryEvent);
        log.info("Successfully saved Library Event {}", savedLibraryEvent);
    }
}
