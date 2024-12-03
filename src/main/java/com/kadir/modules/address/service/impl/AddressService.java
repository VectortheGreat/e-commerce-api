package com.kadir.modules.address.service.impl;

import com.kadir.common.utils.merge.MergeUtils;
import com.kadir.common.utils.pagination.PaginationUtils;
import com.kadir.common.utils.pagination.RestPageableEntity;
import com.kadir.modules.address.dto.AddressCreateDto;
import com.kadir.modules.address.dto.AddressDto;
import com.kadir.modules.address.dto.AddressUpdateDto;
import com.kadir.modules.address.model.Address;
import com.kadir.modules.address.repository.AddressRepository;
import com.kadir.modules.address.service.IAddressService;
import com.kadir.modules.authentication.model.User;
import com.kadir.modules.authentication.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressService implements IAddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Override
    public AddressDto createAddress(AddressCreateDto addressCreateDto) {
        User user = userRepository.findById(addressCreateDto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Address address = modelMapper.map(addressCreateDto, Address.class);
        address.setUser(user);
        Address savedAddress = addressRepository.save(address);
        return modelMapper.map(savedAddress, AddressDto.class);
    }

    @Override
    public AddressDto updateAddress(Long id, AddressUpdateDto addressUpdateDto) {
        Address existingAddress = addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        if (addressUpdateDto.getUserId() != null) {
            User user = userRepository.findById(addressUpdateDto.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            existingAddress.setUser(user);
        }
        MergeUtils.copyNonNullProperties(addressUpdateDto, existingAddress);
        Address savedAddress = addressRepository.save(existingAddress);
        return modelMapper.map(savedAddress, AddressDto.class);
    }

    @Transactional
    @Override
    public AddressDto deleteAddress(Long id) {
        Address existingAddress = addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Address not found"));
        Address address = modelMapper.map(existingAddress, Address.class);
        addressRepository.deleteById(id);
        AddressDto addressDto = modelMapper.map(address, AddressDto.class);
        return addressDto;
    }

    @Override
    public List<AddressDto> getUserAddresses(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Address> addresses = addressRepository.findByUserId(userId);
        List<AddressDto> addressDtos = addresses.stream()
                .map(address -> modelMapper.map(address, AddressDto.class))
                .collect(Collectors.toList());

        return addressDtos;
    }


    @Override
    public RestPageableEntity<AddressDto> getAllAddress(int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by("createdAt").descending());
        Page<Address> addressPage = addressRepository.findAll(pageable);
        RestPageableEntity<AddressDto> pageableResponse = PaginationUtils.toPageableResponse(addressPage, AddressDto.class, modelMapper);
        pageableResponse.setDocs(addressPage.getContent().stream()
                .map(product -> modelMapper.map(product, AddressDto.class))
                .collect(Collectors.toList()));
        return pageableResponse;
    }
}