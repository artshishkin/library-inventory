package com.artarkatesoft.learnkafka.libraryeventsproducer.producers;

import com.artarkatesoft.learnkafka.libraryeventsproducer.domain.Book;
import com.artarkatesoft.learnkafka.libraryeventsproducer.domain.LibraryEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LibraryEventProducerTest {

    @Mock
    KafkaTemplate<Integer, String> kafkaTemplate;

    @Spy
    ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    LibraryEventProducer libraryEventProducer;

    @Test
    void sendLibraryEventUsingProducerRecord_failure() throws JsonProcessingException {
        //given
        Book book = Book.builder()
                .id(123)
                .author("Art")
                .name("We are the best")
                .build();
        LibraryEvent libraryEvent = LibraryEvent.builder().book(book).build();
        SettableListenableFuture<SendResult<Integer, String>> futureWithEx = new SettableListenableFuture<>();
        futureWithEx.setException(new RuntimeException("Exception calling Kafka"));

        given(kafkaTemplate.send(isA(ProducerRecord.class))).willReturn(futureWithEx);

        //when
        ListenableFuture<SendResult<Integer, String>> future = libraryEventProducer.sendLibraryEventUsingProducerRecord(libraryEvent);

        //then
        assertThatThrownBy(() -> future.get())
                .hasMessageContaining("Exception calling Kafka")
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(RuntimeException.class)
                .hasRootCauseMessage("Exception calling Kafka")
        ;
    }
}