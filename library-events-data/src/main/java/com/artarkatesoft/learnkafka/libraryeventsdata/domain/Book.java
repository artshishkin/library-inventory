package com.artarkatesoft.learnkafka.libraryeventsdata.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book {
    @NotNull
    private Integer id;
    @NotEmpty
    private String name;
    @NotEmpty
    private String author;
}
