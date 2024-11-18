package com.kadir.dto;


import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DtoProductIU {

    @NotNull(message = "Name is required")
    private String name;


    private String description;

    private double price;

    private int stockQuantity;

    private Long categoryId;
}
