package com.artarkatesoft.learnkafka.libraryeventsproducer.controllers;

import com.artarkatesoft.learnkafka.libraryeventsproducer.domain.Book;
import com.artarkatesoft.learnkafka.libraryeventsproducer.domain.LibraryEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.admin.properties.bootstrap.servers=${spring.embedded.kafka.brokers}"
})
@EmbeddedKafka(topics = {"library-events"}, partitions = 3)
class LibraryEventsControllerIT {

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void newLibraryEvent() {
        //given
        URI url = URI.create(LibraryEventsController.BASE_URL);

        Book book = Book.builder()
                .id(123)
                .author("Art")
                .name("We are the best")
                .build();
        LibraryEvent libraryEvent = LibraryEvent.builder().book(book).build();

        //when
        ResponseEntity<LibraryEvent> responseEntity = restTemplate
                .postForEntity(url, libraryEvent, LibraryEvent.class);

        //then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        LibraryEvent responseLibraryEvent = responseEntity.getBody();
        assertThat(responseLibraryEvent.getBook().getAuthor()).isEqualTo("Art");

    }
}