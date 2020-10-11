package com.artarkatesoft.learnkafka.libraryeventsconsumer.consumers;

import com.artarkatesoft.learnkafka.libraryeventsconsumer.entity.Book;
import com.artarkatesoft.learnkafka.libraryeventsconsumer.entity.LibraryEvent;
import com.artarkatesoft.learnkafka.libraryeventsconsumer.repositories.LibraryEventsRepository;
import com.artarkatesoft.learnkafka.libraryeventsconsumer.services.LibraryEventsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.then;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
@EmbeddedKafka(topics = {"library-events"}, partitions = 3)
class LibraryEventsConsumerIT {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    EmbeddedKafkaBroker embeddedKafkaBroker;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    KafkaTemplate<Integer, String> kafkaTemplate;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @SpyBean
    LibraryEventsConsumer libraryEventsConsumerSpy;

    @SpyBean
    LibraryEventsService libraryEventsServiceSpy;

    @Autowired
    LibraryEventsRepository repository;

    @BeforeEach
    void setUp() {
        for (MessageListenerContainer listenerContainer : kafkaListenerEndpointRegistry.getAllListenerContainers()) {
            ContainerTestUtils.waitForAssignment(listenerContainer, embeddedKafkaBroker.getPartitionsPerTopic());
        }
    }

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    @Test
    void publishNewLibraryEvent() throws ExecutionException, InterruptedException, JsonProcessingException {
        //given
        String json = "{\"libraryEventId\":null,\"libraryEventType\":\"NEW\",\"book\":{\"id\":456,\"name\":\"Kafka Using Spring Boot\",\"author\":\"Art\"}}";

        //when
        kafkaTemplate.sendDefault(json).get();

        CountDownLatch latch = new CountDownLatch(1);
        latch.await(3, TimeUnit.SECONDS);

        //then
        then(libraryEventsConsumerSpy).should().onMessage(isA(ConsumerRecord.class));
        then(libraryEventsServiceSpy).should().processLibraryEvents(isA(ConsumerRecord.class));

        assertThat(repository.count()).isGreaterThan(0);
        repository.findAll().forEach(libraryEvent -> assertAll(
                () -> assertThat(libraryEvent.getBook().getId()).isEqualTo(456),
                () -> assertThat(libraryEvent.getBook().getName()).isEqualTo("Kafka Using Spring Boot"),
                () -> assertThat(libraryEvent.getBook().getAuthor()).isEqualTo("Art")
        ));
    }

    @Test
    void publishUpdateLibraryEvent() throws ExecutionException, InterruptedException, JsonProcessingException {
        //given
        Book book = Book.builder()
                .id(123)
                .author("Art")
                .name("We are the best")
                .build();
        LibraryEvent libraryEventToBeSavedInDB = LibraryEvent.builder().book(book).build();
        book.setLibraryEvent(libraryEventToBeSavedInDB);

        LibraryEvent savedEvent = repository.save(libraryEventToBeSavedInDB);
        Integer libraryEventId = savedEvent.getLibraryEventId();

        String json = "{\"libraryEventId\":" + libraryEventId + ",\"libraryEventType\":\"UPDATE\",\"book\":{\"id\":123,\"name\":\"We are TESTING Kafka\",\"author\":\"NoArt\"}}";

        //when
        kafkaTemplate.sendDefault(libraryEventId, json).get();

        CountDownLatch latch = new CountDownLatch(1);
        latch.await(3, TimeUnit.SECONDS);

        //then
        then(libraryEventsConsumerSpy).should().onMessage(isA(ConsumerRecord.class));
        then(libraryEventsServiceSpy).should().processLibraryEvents(isA(ConsumerRecord.class));

        assertThat(repository.count()).isEqualTo(1);
        LibraryEvent libraryEventUpdated = repository.findById(libraryEventId).get();
        assertAll(
                () -> assertThat(libraryEventUpdated.getBook().getId()).isEqualTo(123),
                () -> assertThat(libraryEventUpdated.getBook().getName()).isEqualTo("We are TESTING Kafka"),
                () -> assertThat(libraryEventUpdated.getBook().getAuthor()).isEqualTo("NoArt")
        );
    }
}
