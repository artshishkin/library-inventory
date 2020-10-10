package com.artarkatesoft.learnkafka.libraryeventsproducer.controllers;

import com.artarkatesoft.learnkafka.libraryeventsproducer.domain.Book;
import com.artarkatesoft.learnkafka.libraryeventsproducer.domain.LibraryEvent;
import com.artarkatesoft.learnkafka.libraryeventsproducer.domain.LibraryEventType;
import com.artarkatesoft.learnkafka.libraryeventsproducer.producers.LibraryEventProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.support.SendResult;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.util.StringUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;
import org.springframework.web.server.ResponseStatusException;

import static com.artarkatesoft.learnkafka.libraryeventsproducer.producers.LibraryEventProducer.TOPIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LibraryEventsController.class)
class LibraryEventsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    LibraryEventProducer libraryEventProducer;

    @Captor
    ArgumentCaptor<LibraryEvent> eventArgumentCaptor;

    @Test
    void newLibraryEvent() throws Exception {

        //given
        Book book = Book.builder()
                .id(123)
                .author("Art")
                .name("We are the best")
                .build();
        LibraryEvent libraryEvent = LibraryEvent.builder().book(book).build();
        String jsonEvent = objectMapper.writeValueAsString(libraryEvent);

        ListenableFuture<SendResult<Integer, String>> future = Mockito.mock(ListenableFuture.class);
        given(libraryEventProducer.sendLibraryEventUsingProducerRecord(any(LibraryEvent.class))).willReturn(future);

        //when
        mockMvc.perform(
                post(LibraryEventsController.BASE_URL)
                        .accept(APPLICATION_JSON)
                        .contentType(APPLICATION_JSON)
                        .content(jsonEvent))

                //then
                .andExpect(status().isCreated())
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(jsonPath("$.book.id", equalTo(123)))
                .andExpect(jsonPath("$.book.author", equalTo("Art")))
                .andExpect(jsonPath("$.book.name", equalTo("We are the best")))
        ;

        then(libraryEventProducer).should().sendLibraryEventUsingProducerRecord(eventArgumentCaptor.capture());
        LibraryEvent captorValue = eventArgumentCaptor.getValue();
        assertAll(
                () -> assertThat(captorValue.getBook()).isEqualTo(book),
                () -> assertThat(captorValue.getLibraryEventType()).isEqualTo(LibraryEventType.NEW)
        );
    }

    @Test
    void newLibraryEvent_givenNullBook() throws Exception {

        //given
        LibraryEvent libraryEvent = LibraryEvent.builder().book(null).build();
        String jsonEvent = objectMapper.writeValueAsString(libraryEvent);

        //when
        mockMvc.perform(
                post(LibraryEventsController.BASE_URL)
                        .accept(APPLICATION_JSON)
                        .contentType(APPLICATION_JSON)
                        .content(jsonEvent))

                //then
                .andExpect(status().is4xxClientError())
                .andExpect(MockMvcResultMatchers.content().string("book - must not be null"));

        then(libraryEventProducer).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @CsvSource({
            ",name1,auth1",
            "123,,auth1",
            "123,name1,",
            ",,auth1",
            "123,,",
            ",,"})
    void newLibraryEvent_givenWrongBookAttributes(Integer bookId, String bookName, String bookAuthor) throws Exception {

        //given
        Book book = Book.builder()
                .id(bookId)
                .author(bookAuthor)
                .name(bookName)
                .build();

        LibraryEvent libraryEvent = LibraryEvent.builder().book(book).build();
        String jsonEvent = objectMapper.writeValueAsString(libraryEvent);

        //when
        MvcResult mvcResult = mockMvc.perform(
                post(LibraryEventsController.BASE_URL)
                        .accept(APPLICATION_JSON)
                        .contentType(APPLICATION_JSON)
                        .content(jsonEvent))

                //then
                .andExpect(status().is4xxClientError())
                .andReturn();

        then(libraryEventProducer).shouldHaveNoInteractions();
        String responseContent = mvcResult.getResponse().getContentAsString();

        if (bookId == null)
            assertThat(responseContent).contains("book.id - must not be null");
        if (StringUtils.isEmpty(bookName))
            assertThat(responseContent).contains("book.name - must not be empty");
        if (StringUtils.isEmpty(bookAuthor))
            assertThat(responseContent).contains("book.author - must not be empty");
    }

    @Test
    void updateLibraryEvent() throws Exception {

        //given
        Book book = Book.builder()
                .id(123)
                .author("Art")
                .name("We are the best")
                .build();
        LibraryEvent libraryEvent = LibraryEvent.builder().libraryEventId(321).book(book).build();
        String jsonEvent = objectMapper.writeValueAsString(libraryEvent);

        int stubPartition = 2;
        Integer key = libraryEvent.getLibraryEventId();
        String value = jsonEvent;

        SettableListenableFuture<SendResult<Integer, String>> future = new SettableListenableFuture<>();
        ProducerRecord<Integer, String> stubProducerRecord = new ProducerRecord<>(LibraryEventProducer.TOPIC, stubPartition, System.currentTimeMillis(), key, value);
        TopicPartition stubTopicPartition = new TopicPartition(TOPIC, stubPartition);
        RecordMetadata stubRecordMetadata = new RecordMetadata(stubTopicPartition, 1, 1, System.currentTimeMillis(), 123L, 123, 123);

        SendResult<Integer, String> stubSendResult = new SendResult<>(stubProducerRecord, stubRecordMetadata);
        future.set(stubSendResult);
        given(libraryEventProducer.sendLibraryEventUsingProducerRecord(any(LibraryEvent.class))).willReturn(future);

        //when
        mockMvc.perform(
                put(LibraryEventsController.BASE_URL)
                        .accept(APPLICATION_JSON)
                        .contentType(APPLICATION_JSON)
                        .content(jsonEvent))

                //then
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(jsonPath("$.libraryEventId", equalTo(321)))
                .andExpect(jsonPath("$.libraryEventType", equalTo("UPDATE")))
                .andExpect(jsonPath("$.book.id", equalTo(123)))
                .andExpect(jsonPath("$.book.author", equalTo("Art")))
                .andExpect(jsonPath("$.book.name", equalTo("We are the best")))
        ;

        then(libraryEventProducer).should().sendLibraryEventUsingProducerRecord(eventArgumentCaptor.capture());
        LibraryEvent captorValue = eventArgumentCaptor.getValue();
        assertAll(
                () -> assertThat(captorValue.getBook()).isEqualTo(book),
                () -> assertThat(captorValue.getLibraryEventType()).isEqualTo(LibraryEventType.UPDATE)
        );
    }

    @Test
    void updateLibraryEvent_withMockingListenableFutureFromProducer() throws Exception {

        //given
        Book book = Book.builder()
                .id(123)
                .author("Art")
                .name("We are the best")
                .build();
        LibraryEvent libraryEvent = LibraryEvent.builder().libraryEventId(321).book(book).build();
        String jsonEvent = objectMapper.writeValueAsString(libraryEvent);

        ListenableFuture<SendResult<Integer, String>> future = Mockito.mock(ListenableFuture.class);
        given(libraryEventProducer.sendLibraryEventUsingProducerRecord(any(LibraryEvent.class))).willReturn(future);

        //when
        mockMvc.perform(
                put(LibraryEventsController.BASE_URL)
                        .accept(APPLICATION_JSON)
                        .contentType(APPLICATION_JSON)
                        .content(jsonEvent))

                //then
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(jsonPath("$.libraryEventId", equalTo(321)))
                .andExpect(jsonPath("$.libraryEventType", equalTo("UPDATE")))
                .andExpect(jsonPath("$.book.id", equalTo(123)))
                .andExpect(jsonPath("$.book.author", equalTo("Art")))
                .andExpect(jsonPath("$.book.name", equalTo("We are the best")))
        ;

        then(libraryEventProducer).should().sendLibraryEventUsingProducerRecord(eventArgumentCaptor.capture());
        LibraryEvent captorValue = eventArgumentCaptor.getValue();
        assertAll(
                () -> assertThat(captorValue.getBook()).isEqualTo(book),
                () -> assertThat(captorValue.getLibraryEventType()).isEqualTo(LibraryEventType.UPDATE)
        );
    }

    @Test
    void updateLibraryEvent_withNullLibraryEventId() throws Exception {

        //given
        Book book = Book.builder()
                .id(123)
                .author("Art")
                .name("We are the best")
                .build();
        LibraryEvent libraryEvent = LibraryEvent.builder().book(book).build();
        String jsonEvent = objectMapper.writeValueAsString(libraryEvent);

        //when
        MvcResult mvcResult = mockMvc.perform(
                put(LibraryEventsController.BASE_URL)
                        .accept(APPLICATION_JSON)
                        .contentType(APPLICATION_JSON)
                        .content(jsonEvent))

                //then
                .andExpect(status().isBadRequest())
                .andReturn();
//Not working without custom ExceptionHandler
//                .andExpect(content().contentType(APPLICATION_JSON))
//                .andExpect(jsonPath("$.status", equalTo(400)))
//                .andExpect(jsonPath("$.error", equalTo("Bad Request")))
//                .andExpect(jsonPath("$.message", equalTo("Please pass the LibraryEventId")))
//                .andExpect(jsonPath("$.path", equalTo(LibraryEventsController.BASE_URL)))
//        ;
        then(libraryEventProducer).shouldHaveNoInteractions();
        MockHttpServletResponse response = mvcResult.getResponse();
        String errorMessage = response.getErrorMessage();
        Exception resolvedException = mvcResult.getResolvedException();
        assertThat(errorMessage).isEqualTo("Please pass the LibraryEventId");
        assertThat(resolvedException)
                .isExactlyInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }
}