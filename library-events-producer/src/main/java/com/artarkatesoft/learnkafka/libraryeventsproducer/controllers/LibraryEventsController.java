package com.artarkatesoft.learnkafka.libraryeventsproducer.controllers;

import com.artarkatesoft.learnkafka.libraryeventsproducer.domain.LibraryEvent;
import com.artarkatesoft.learnkafka.libraryeventsproducer.producers.LibraryEventProducer;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.*;

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
    public LibraryEvent newLibraryEvent(@RequestBody LibraryEvent libraryEvent) throws JsonProcessingException, ExecutionException, InterruptedException, TimeoutException {
//        SendResult<Integer, String> sendResult = libraryEventProducer.sendLibraryEventSynchronous(libraryEvent);
        ListenableFuture<SendResult<Integer, String>> future = libraryEventProducer.sendLibraryEventUsingProducerRecord(libraryEvent);
        SendResult<Integer, String> sendResult = future.get(1, TimeUnit.SECONDS);
        log.info("SendResult is {}", sendResult);
        return libraryEvent;
    }
}
