package com.artarkatesoft.learnkafka.libraryeventsproducer.controllers;

import com.artarkatesoft.learnkafka.libraryeventsproducer.domain.LibraryEvent;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(LibraryEventsController.BASE_URL)
public class LibraryEventsController {

    static final String BASE_URL = "/v1/libraryevents";

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LibraryEvent newLibraryEvent(@RequestBody LibraryEvent libraryEvent) {

//invoke kafka producer
        return libraryEvent;
    }
}
