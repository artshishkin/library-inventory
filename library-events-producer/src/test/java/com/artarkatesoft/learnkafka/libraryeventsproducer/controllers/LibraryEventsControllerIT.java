package com.artarkatesoft.learnkafka.libraryeventsproducer.controllers;

import com.artarkatesoft.learnkafka.libraryeventsproducer.domain.Book;
import com.artarkatesoft.learnkafka.libraryeventsproducer.domain.LibraryEvent;
import com.artarkatesoft.learnkafka.libraryeventsproducer.domain.LibraryEventType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.admin.properties.bootstrap.servers=${spring.embedded.kafka.brokers}"
})
@EmbeddedKafka(topics = {"library-events"}, partitions = 3)
class LibraryEventsControllerIT {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    ObjectMapper objectMapper;

    Consumer<Integer, String> consumer;

    @BeforeEach
    void setUp() {
        Map<String, Object> configs = KafkaTestUtils.consumerProps("group1", "true", embeddedKafkaBroker);
        DefaultKafkaConsumerFactory<Integer, String> factory = new DefaultKafkaConsumerFactory<>(configs, new IntegerDeserializer(), new StringDeserializer());
        consumer = factory.createConsumer();
        embeddedKafkaBroker.consumeFromAllEmbeddedTopics(consumer);
    }

    @AfterEach
    void tearDown() {
        consumer.close();
    }

    @Test
    void newLibraryEvent() throws JsonProcessingException {
        //given
        URI url = URI.create(LibraryEventsController.BASE_URL);

        Book book = Book.builder()
                .id(123)
                .author("Art")
                .name("We are the best")
                .build();
        LibraryEvent libraryEvent = LibraryEvent.builder().book(book).build();
//        String expectedKafkaValue = objectMapper.writeValueAsString(libraryEvent);
        String expectedKafkaValue = "{\"libraryEventId\":null,\"libraryEventType\":\"NEW\",\"book\":{\"id\":123,\"name\":\"We are the best\",\"author\":\"Art\"}}";

        //when
        ResponseEntity<LibraryEvent> responseEntity = restTemplate
                .postForEntity(url, libraryEvent, LibraryEvent.class);

        //then
        assertThat(responseEntity.getStatusCode()).isEqualTo(CREATED);
        LibraryEvent responseLibraryEvent = responseEntity.getBody();
        assertThat(responseLibraryEvent.getBook().getAuthor()).isEqualTo("Art");

        ConsumerRecord<Integer, String> consumerRecord = KafkaTestUtils.getSingleRecord(consumer, "library-events");
        String value = consumerRecord.value();
        assertThat(value).isEqualTo(expectedKafkaValue);

    }

    @Test
    void updateLibraryEvent_success() {
        //given
        Book book = Book.builder()
                .id(123)
                .author("Art")
                .name("We are the best")
                .build();
        LibraryEvent libraryEvent = LibraryEvent.builder().libraryEventId(654).book(book).build();
        String expectedKafkaValue = "{\"libraryEventId\":654,\"libraryEventType\":\"UPDATE\",\"book\":{\"id\":123,\"name\":\"We are the best\",\"author\":\"Art\"}}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_JSON);
        headers.setAccept(List.of(APPLICATION_JSON));

        HttpEntity<LibraryEvent> requestEntity = new HttpEntity<>(libraryEvent, headers);

        //when
        ResponseEntity<LibraryEvent> responseEntity = restTemplate
                .exchange(LibraryEventsController.BASE_URL, PUT, requestEntity, LibraryEvent.class);

        //then
        assertThat(responseEntity.getStatusCode()).isEqualTo(OK);
        LibraryEvent responseLibraryEvent = responseEntity.getBody();
        assertThat(responseLibraryEvent.getBook().getAuthor()).isEqualTo("Art");
        assertThat(responseLibraryEvent.getLibraryEventType()).isEqualTo(LibraryEventType.UPDATE);

        ConsumerRecord<Integer, String> consumerRecord = KafkaTestUtils.getSingleRecord(consumer, "library-events");
        String value = consumerRecord.value();
        assertThat(value).isEqualTo(expectedKafkaValue);
    }

    @Test
    void updateLibraryEvent_failure() {
        //given
        Book book = Book.builder()
                .id(123)
                .author("Art")
                .name("We are the best")
                .build();
        LibraryEvent libraryEvent = LibraryEvent.builder().book(book).build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_JSON);
        headers.setAccept(List.of(APPLICATION_JSON));

        HttpEntity<LibraryEvent> requestEntity = new HttpEntity<>(libraryEvent, headers);

        //when
        ResponseEntity<String> responseEntity = restTemplate
                .exchange(LibraryEventsController.BASE_URL, PUT, requestEntity, String.class);

        //then
        assertThat(responseEntity.getStatusCode()).isEqualTo(BAD_REQUEST);
        String responseString = responseEntity.getBody();
        List<String> errorMessageParts = List.of(
                "\"status\":400",
                "\"error\":\"Bad Request\"",
                "\"message\":\"Please pass the LibraryEventId\"",
                "\"path\":\"" + LibraryEventsController.BASE_URL + "\"");
        assertThat(responseString).contains(errorMessageParts);
    }
}