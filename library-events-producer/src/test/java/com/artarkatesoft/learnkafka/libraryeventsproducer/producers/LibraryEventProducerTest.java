package com.artarkatesoft.learnkafka.libraryeventsproducer.producers;

import com.artarkatesoft.learnkafka.libraryeventsdata.domain.Book;
import com.artarkatesoft.learnkafka.libraryeventsdata.domain.LibraryEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static com.artarkatesoft.learnkafka.libraryeventsproducer.producers.LibraryEventProducer.TOPIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class LibraryEventProducerTest {

    @Mock
    KafkaTemplate<Integer, String> kafkaTemplate;

    @Spy
    ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    LibraryEventProducer libraryEventProducer;

    @Captor
    ArgumentCaptor<ProducerRecord<Integer, String>> producerRecordCaptor;

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

    @Test
    void sendLibraryEventUsingProducerRecord_success() throws JsonProcessingException, ExecutionException, InterruptedException, TimeoutException {
        //given
        Book book = Book.builder()
                .id(123)
                .author("Art")
                .name("We are the best")
                .build();
        LibraryEvent libraryEvent = LibraryEvent.builder().book(book).build();
        SettableListenableFuture<SendResult<Integer, String>> futureStub = new SettableListenableFuture<>();
        String jsonEvent = objectMapper.writeValueAsString(libraryEvent);
        ProducerRecord<Integer, String> producerRecord = new ProducerRecord<>(TOPIC, null, jsonEvent);

        int stubPartition = 2;
        TopicPartition stubTopicPartition = new TopicPartition(TOPIC, stubPartition);
        RecordMetadata stubRecordMetadata = new RecordMetadata(stubTopicPartition, 1, 1, System.currentTimeMillis(), 123L, 123, 123);
        SendResult<Integer, String> sendResultStub = new SendResult<>(producerRecord, stubRecordMetadata);
        futureStub.set(sendResultStub);

        given(kafkaTemplate.send(isA(ProducerRecord.class))).willReturn(futureStub);

        //when
        ListenableFuture<SendResult<Integer, String>> future = libraryEventProducer.sendLibraryEventUsingProducerRecord(libraryEvent);

        //then
        SendResult<Integer, String> sendResult = future.get();
        then(kafkaTemplate).should().send(producerRecordCaptor.capture());

        assertThat(sendResult).isEqualTo(sendResultStub);

        ProducerRecord<Integer, String> captorValue = producerRecordCaptor.getValue();
        assertAll(
                () -> assertThat(captorValue.headers().headers("event-source")).hasSize(1),
                () -> assertThat(captorValue.key()).isNull(),
                () -> assertThat(captorValue.topic()).isEqualTo(TOPIC),
                () -> assertThat(captorValue.value()).isEqualTo(jsonEvent)
        );
    }
}
