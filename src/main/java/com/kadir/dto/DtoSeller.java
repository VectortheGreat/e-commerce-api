package com.kadir.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DtoSeller extends DtoBase {

    @NotNull
    private DtoUser userId;

    @NotNull
    private String companyName;

}

