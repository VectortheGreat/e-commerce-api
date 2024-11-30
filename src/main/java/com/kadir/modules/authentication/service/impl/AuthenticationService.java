package com.kadir.modules.authentication.service.impl;

import com.kadir.common.enums.UserRole;
import com.kadir.common.exception.BaseException;
import com.kadir.common.exception.ErrorMessage;
import com.kadir.common.exception.MessageType;
import com.kadir.common.jwt.JWTService;
import com.kadir.modules.authentication.dto.*;
import com.kadir.modules.authentication.model.Customer;
import com.kadir.modules.authentication.model.RefreshToken;
import com.kadir.modules.authentication.model.Seller;
import com.kadir.modules.authentication.model.User;
import com.kadir.modules.authentication.repository.CustomerRepository;
import com.kadir.modules.authentication.repository.RefreshTokenRepository;
import com.kadir.modules.authentication.repository.SellerRepository;
import com.kadir.modules.authentication.repository.UserRepository;
import com.kadir.modules.authentication.service.IAuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationService implements IAuthenticationService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final SellerRepository sellerRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final AuthenticationProvider authenticationProvider;
    private final JWTService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;

    private User createUser(AuthRegisterRequest input, UserRole role) {
        User user = new User();
        user.setUsername(input.getUsername());
        user.setPassword(bCryptPasswordEncoder.encode(input.getPassword()));
        user.setRole(role);
        user.setEmail(input.getEmail());
        user.setPhoneNumber(input.getPhoneNumber());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setAddress(input.getAddress());
        return user;
    }

    private Customer createCustomer(AuthCustomerRegisterRequest input, User user) {
        Customer customer = new Customer();
        customer.setFirstName(input.getFirstName());
        customer.setLastName(input.getLastName());
        customer.setUser(user);
        customer.setCreatedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());
        return customer;
    }

    private Seller createSeller(AuthSellerRegisterRequest input, User user) {
        Seller seller = new Seller();
        seller.setUser(user);
        seller.setCompanyName(input.getCompanyName());
        seller.setCreatedAt(LocalDateTime.now());
        seller.setUpdatedAt(LocalDateTime.now());
        return seller;
    }

    private void validateUser(AuthRegisterRequest input, Boolean isCustomer) {
        if (userRepository.existsByUsername(input.getUsername())) {
            throw new BaseException(new ErrorMessage(MessageType.USERNAME_ALREADY_EXISTS, input.getUsername()));
        }

        if (userRepository.existsByEmail(input.getEmail())) {
            throw new BaseException(new ErrorMessage(MessageType.EMAIL_ALREADY_EXISTS, input.getEmail()));
        }

        if (userRepository.existsByPhoneNumber(input.getPhoneNumber())) {
            throw new BaseException(new ErrorMessage(MessageType.PHONE_NUMBER_ALREADY_EXISTS, input.getPhoneNumber()));
        }

        if (isCustomer) {
            AuthCustomerRegisterRequest customerInput = (AuthCustomerRegisterRequest) input;

            if (customerInput.getFirstName() == null || customerInput.getFirstName().isEmpty() ||
                    customerInput.getLastName() == null || customerInput.getLastName().isEmpty()) {
                throw new BaseException(new ErrorMessage(MessageType.FIRST_NAME_AND_LAST_NAME_REQUIRED, ""));
            }
        } else {
            AuthSellerRegisterRequest sellerInput = (AuthSellerRegisterRequest) input;

            if (sellerInput.getCompanyName() == null || sellerInput.getCompanyName().isEmpty()) {
                throw new BaseException(new ErrorMessage(MessageType.COMPANY_NAME_REQUIRED, ""));
            }
        }
    }


    private RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setExpiredDate(LocalDateTime.now().plusHours(4));
        refreshToken.setRefreshToken(UUID.randomUUID().toString());
        refreshToken.setUser(user);
        refreshToken.setCreatedAt(LocalDateTime.now());
        refreshToken.setUpdatedAt(LocalDateTime.now());
        return refreshToken;
    }

    public boolean isValidRefreshToken(LocalDateTime expiredDate) {
        return LocalDateTime.now().isBefore(expiredDate);
    }


    @Override
    public DtoCustomer registerCustomer(AuthCustomerRegisterRequest input) {
        validateUser(input, true);
        DtoCustomer dtoCustomer = new DtoCustomer();
        DtoUser dtoUser = new DtoUser();
        User savedUser = userRepository.save(createUser(input, UserRole.CUSTOMER));
        Customer customer = createCustomer(input, savedUser);
        customerRepository.save(customer);
        BeanUtils.copyProperties(customer, dtoCustomer);

        dtoUser.setId(savedUser.getId());
        dtoUser.setUsername(savedUser.getUsername());
        dtoUser.setEmail(savedUser.getEmail());
        dtoUser.setPhoneNumber(savedUser.getPhoneNumber());
        dtoUser.setRole(savedUser.getRole());
        dtoUser.setAddress(savedUser.getAddress());

        dtoCustomer.setFirstName(customer.getFirstName());
        dtoCustomer.setLastName(customer.getLastName());
        dtoCustomer.setUser(dtoUser);
        return dtoCustomer;
    }

    @Override
    public DtoSeller registerSeller(AuthSellerRegisterRequest input) {
        validateUser(input, false);
        DtoSeller dtoSeller = new DtoSeller();
        DtoUser dtoUser = new DtoUser();
        User savedUser = userRepository.save(createUser(input, UserRole.SELLER));
        Seller seller = createSeller(input, savedUser);
        sellerRepository.save(seller);
        BeanUtils.copyProperties(seller, dtoSeller);

        dtoUser.setId(savedUser.getId());
        dtoUser.setUsername(savedUser.getUsername());
        dtoUser.setEmail(savedUser.getEmail());
        dtoUser.setPhoneNumber(savedUser.getPhoneNumber());
        dtoUser.setRole(savedUser.getRole());
        dtoUser.setAddress(savedUser.getAddress());

        dtoSeller.setCompanyName(seller.getCompanyName());
        dtoSeller.setUser(dtoUser);
        return dtoSeller;
    }


    @Override
    public AuthResponse authenticate(AuthRequest input) {
        try {
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(input.getUsername(), input.getPassword());
            authenticationProvider.authenticate(authenticationToken);

            User user = userRepository.findByUsername(input.getUsername())
                    .orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.USERNAME_OR_PASSWORD_INVALID, input.getUsername())));

            String accessToken = jwtService.generateToken(user);
            RefreshToken savedRefreshToken = refreshTokenRepository.save(createRefreshToken(user));
            return new AuthResponse(accessToken, savedRefreshToken.getRefreshToken());
        } catch (Exception e) {
            throw new BaseException(new ErrorMessage(MessageType.USERNAME_OR_PASSWORD_INVALID, e.getMessage()));
        }
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest input) {
        Optional<RefreshToken> optionalRefreshToken = refreshTokenRepository.findByRefreshToken(input.getRefreshToken());
        if (optionalRefreshToken.isEmpty()) {
            throw new BaseException(new ErrorMessage(MessageType.REFRESH_TOKEN_NOT_FOUND, input.getRefreshToken()));
        }
        if (!isValidRefreshToken(optionalRefreshToken.get().getExpiredDate())) {
            throw new BaseException(new ErrorMessage(MessageType.REFRESH_TOKEN_IS_EXPIRED, input.getRefreshToken()));
        }
        User user = optionalRefreshToken.get().getUser();
        String accessToken = jwtService.generateToken(user);
        RefreshToken savedRefreshToken = refreshTokenRepository.save(createRefreshToken(user));
        return new AuthResponse(accessToken, savedRefreshToken.getRefreshToken());
    }

}