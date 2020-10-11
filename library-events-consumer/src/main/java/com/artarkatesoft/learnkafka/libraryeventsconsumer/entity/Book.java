package com.artarkatesoft.learnkafka.libraryeventsconsumer.entity;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Book {
    @Id
    private Integer id;
    private String name;
    private String author;

    @OneToOne
    @JoinColumn(name = "libraryEventId")
    @ToString.Exclude
    LibraryEvent libraryEvent;
}
