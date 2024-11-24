package com.kadir.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DtoBase {

    private Long id;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
