package com.artarkatesoft.learnkafka.libraryeventsproducer.controllers;

import com.artarkatesoft.learnkafka.libraryeventsproducer.domain.LibraryEvent;
import com.artarkatesoft.learnkafka.libraryeventsproducer.producers.LibraryEventProducer;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping(LibraryEventsController.BASE_URL)
public class LibraryEventsController {

    static final String BASE_URL = "/v1/libraryevents";

    private final LibraryEventProducer libraryEventProducer;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LibraryEvent newLibraryEvent(@RequestBody LibraryEvent libraryEvent) throws JsonProcessingException {
        libraryEventProducer.sendLibraryEvent(libraryEvent);
        return libraryEvent;
    }
}
