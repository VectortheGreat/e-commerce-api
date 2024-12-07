package com.kadir.modules.favoriteproduct.dto;

import com.kadir.common.dto.DtoBase;
import com.kadir.modules.authentication.dto.UserDto;
import com.kadir.modules.product.dto.ProductDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteProductDto extends DtoBase {

    private UserDto user;

    private ProductDto product;

    private boolean isActive;
}