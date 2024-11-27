package com.kadir.dto;


import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Data
public class DtoProductIU {

    @NotNull(message = "Name is required")
    private String name;

    private String description;

    private BigDecimal price = BigDecimal.ZERO;

    private int stockQuantity = 0;

    @NotNull(message = "Category is required")
    private Long categoryId;

}
