package com.artarkatesoft.learnkafka.libraryeventsconsumer.repositories;

import com.artarkatesoft.learnkafka.libraryeventsconsumer.entity.LibraryEvent;
import org.springframework.data.repository.CrudRepository;

public interface LibraryEventsRepository extends CrudRepository<LibraryEvent, Integer> {
}
