package com.kadir.modules.authentication.dto;

import com.kadir.common.dto.DtoBase;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDto extends DtoBase {

    @NotNull
    private UserDto user;

    @NotNull
    private String firstName;

    @NotNull
    private String lastName;
}