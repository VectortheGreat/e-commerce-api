package com.kadir.modules.product.service.impl;

import com.kadir.common.exception.BaseException;
import com.kadir.common.exception.ErrorMessage;
import com.kadir.common.exception.MessageType;
import com.kadir.common.utils.pagination.PaginationUtils;
import com.kadir.common.utils.pagination.RestPageableEntity;
import com.kadir.modules.category.model.Category;
import com.kadir.modules.category.repository.CategoryRepository;
import com.kadir.modules.product.dto.DtoProduct;
import com.kadir.modules.product.dto.DtoProductIU;
import com.kadir.modules.product.model.Product;
import com.kadir.modules.product.repository.ProductRepository;
import com.kadir.modules.product.service.IProductService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService implements IProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ModelMapper modelMapper;

    @Override
    public DtoProduct createProduct(DtoProductIU dtoProductIU) {
        categoryRepository.findById(dtoProductIU.getCategoryId())
                .orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.GENERAL_EXCEPTION, "Category not found")));
        Product product = modelMapper.map(dtoProductIU, Product.class);
        Product savedProduct = productRepository.save(product);
        return modelMapper.map(savedProduct, DtoProduct.class);
    }

    @Override
    public DtoProduct updateProduct(Long id, DtoProductIU dtoProductIU) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.GENERAL_EXCEPTION, "Product not found")));
        Category existingCategory = categoryRepository.findById(dtoProductIU.getCategoryId())
                .orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.GENERAL_EXCEPTION, "Category not found")));

        Product updatedProduct = modelMapper.map(dtoProductIU, Product.class);

        updatedProduct.setId(existingProduct.getId());
        updatedProduct.setUpdatedAt(existingProduct.getUpdatedAt());
        updatedProduct.setCreatedAt(existingProduct.getCreatedAt());
        updatedProduct.setCategory(existingCategory);
        Product savedProduct = productRepository.save(updatedProduct);

        return modelMapper.map(savedProduct, DtoProduct.class);
    }


    @Transactional
    @Override
    public DtoProduct deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.GENERAL_EXCEPTION, "Product not found")));
        DtoProduct dtoProduct = modelMapper.map(product, DtoProduct.class);
        productRepository.deleteById(id);
        return dtoProduct;
    }

    @Override
    public RestPageableEntity<DtoProduct> getAllProducts(int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by("createdAt").descending());
        Page<Product> productPage = productRepository.findAll(pageable);
        RestPageableEntity<DtoProduct> pageableResponse = PaginationUtils.toPageableResponse(productPage, DtoProduct.class, modelMapper);
        pageableResponse.setDocs(productPage.getContent().stream()
                .map(product -> modelMapper.map(product, DtoProduct.class))
                .collect(Collectors.toList()));
        return pageableResponse;
    }

    @Override
    public DtoProduct getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.GENERAL_EXCEPTION, "Product not found")));
        Category category = product.getCategory() != null ? categoryRepository.findById(product.getCategory().getId())
                .orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.GENERAL_EXCEPTION, "Category not found"))) : null;
        DtoProduct dtoProduct = modelMapper.map(product, DtoProduct.class);
        dtoProduct.setCategory(category);
        return dtoProduct;
    }

}