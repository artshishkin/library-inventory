package com.artarkatesoft.learnkafka.libraryeventsproducer.controllers;

import com.artarkatesoft.learnkafka.libraryeventsproducer.domain.LibraryEvent;
import com.artarkatesoft.learnkafka.libraryeventsproducer.domain.LibraryEventType;
import com.artarkatesoft.learnkafka.libraryeventsproducer.producers.LibraryEventProducer;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(LibraryEventsController.BASE_URL)
public class LibraryEventsController {

    static final String BASE_URL = "/v1/libraryevents";

    private final LibraryEventProducer libraryEventProducer;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LibraryEvent newLibraryEvent(@RequestBody @Valid LibraryEvent libraryEvent) throws JsonProcessingException, ExecutionException, InterruptedException, TimeoutException {
        libraryEvent.setLibraryEventType(LibraryEventType.NEW);
        ListenableFuture<SendResult<Integer, String>> future = libraryEventProducer.sendLibraryEventUsingProducerRecord(libraryEvent);
        SendResult<Integer, String> sendResult = future.get(1, TimeUnit.SECONDS);
        log.info("SendResult is {}", sendResult);
        return libraryEvent;
    }

    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    public LibraryEvent updateLibraryEvent(@RequestBody @Valid LibraryEvent libraryEvent) throws JsonProcessingException, ExecutionException, InterruptedException, TimeoutException {
        if (libraryEvent.getLibraryEventId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please pass the LibraryEventId");
        libraryEvent.setLibraryEventType(LibraryEventType.UPDATE);
        ListenableFuture<SendResult<Integer, String>> future = libraryEventProducer.sendLibraryEventUsingProducerRecord(libraryEvent);
        SendResult<Integer, String> sendResult = future.get(1, TimeUnit.SECONDS);
        log.info("SendResult is {}", sendResult);
        return libraryEvent;
    }
}
