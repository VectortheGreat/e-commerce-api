package com.kadir.modules.order.service.impl;

import com.kadir.common.enums.OrderStatus;
import com.kadir.common.exception.BaseException;
import com.kadir.common.exception.ErrorMessage;
import com.kadir.common.exception.MessageType;
import com.kadir.common.utils.pagination.PaginationUtils;
import com.kadir.common.utils.pagination.RestPageableEntity;
import com.kadir.modules.authentication.model.User;
import com.kadir.modules.authentication.repository.UserRepository;
import com.kadir.modules.cartitems.model.CartItems;
import com.kadir.modules.cartitems.repository.CartItemsRepository;
import com.kadir.modules.order.dto.DtoOrder;
import com.kadir.modules.order.dto.DtoOrderIU;
import com.kadir.modules.order.model.Order;
import com.kadir.modules.order.repository.OrderRepository;
import com.kadir.modules.order.service.IOrderService;
import com.kadir.modules.orderitems.dto.DtoOrderItems;
import com.kadir.modules.orderitems.model.OrderItems;
import com.kadir.modules.orderitems.repository.OrderItemsRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService implements IOrderService {

    private final OrderRepository orderRepository;
    private final OrderItemsRepository orderItemsRepository;
    private final CartItemsRepository cartItemsRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Transactional
    @Override
    public DtoOrder createOrder(DtoOrderIU dto) {
        User user = userRepository.findById(dto.getUserId()).orElseThrow(
                () -> new BaseException(new ErrorMessage(MessageType.GENERAL_EXCEPTION, "User not found")));
        List<CartItems> cartItems = cartItemsRepository.findByUserId(user.getId());
        if (cartItems.isEmpty()) {
            throw new BaseException(new ErrorMessage(MessageType.GENERAL_EXCEPTION, "Cart is empty"));
        }
        Order order = createAndSaveOrder(user, cartItems);

        List<OrderItems> savedOrderItems = createAndSaveOrderItems(order, cartItems);

        if (!cartItems.isEmpty()) {
            cartItemsRepository.deleteAll(cartItems);
        }

        DtoOrder dtoOrder = modelMapper.map(order, DtoOrder.class);
        dtoOrder.setOrderItems(savedOrderItems.stream()
                .map(item -> modelMapper.map(item, DtoOrderItems.class))
                .collect(Collectors.toSet()));
        return dtoOrder;
    }


    @Override
    public RestPageableEntity<DtoOrder> getAllOrders(int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by("createdAt").descending());
        Page<Order> productPage = orderRepository.findAll(pageable);

        RestPageableEntity<DtoOrder> pageableResponse = PaginationUtils.toPageableResponse(productPage, DtoOrder.class, modelMapper);

        return pageableResponse;
    }

    @Override
    public RestPageableEntity<DtoOrder> getOrdersByUser(Long userId, int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by("createdAt").descending());
        userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.GENERAL_EXCEPTION, "User not found")));
        Page<Order> orderPage = orderRepository.findByUserId(userId, pageable);
        RestPageableEntity<DtoOrder> pageableResponse = PaginationUtils.toPageableResponse(orderPage, DtoOrder.class, modelMapper);
        return pageableResponse;
    }

    @Override
    public DtoOrder updateOrderStatus(Long orderId, OrderStatus paymentStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found."));
        order.setPaymentStatus(paymentStatus);
        Order savedOrder = orderRepository.save(order);

        DtoOrder dtoOrder = modelMapper.map(savedOrder, DtoOrder.class);
        dtoOrder.setOrderItems(savedOrder.getOrderItems().stream()
                .map(orderItem -> modelMapper.map(orderItem, DtoOrderItems.class))
                .collect(Collectors.toSet()));

        return dtoOrder;
    }

    private Order createAndSaveOrder(User user, List<CartItems> cartItems) {
        Order order = new Order();
        order.setUser(user);
        order.setOrderDate(LocalDateTime.now());
        order.setPaymentMethod("CREDIT CARD");
        order.setPaymentStatus(OrderStatus.PENDING);

        BigDecimal totalAmount = cartItems.stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(totalAmount);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        return orderRepository.save(order);
    }

    private List<OrderItems> createAndSaveOrderItems(Order order, List<CartItems> cartItems) {
        List<OrderItems> orderItems = cartItems.stream()
                .map(cartItem -> {
                    OrderItems item = new OrderItems();
                    item.setOrder(order);
                    item.setProduct(cartItem.getProduct());
                    item.setQuantity(cartItem.getQuantity());
                    item.setCreatedAt(LocalDateTime.now());
                    item.setUpdatedAt(LocalDateTime.now());
                    item.setPrice(cartItem.getProduct().getPrice());
                    return item;
                }).collect(Collectors.toList());
        List<OrderItems> savedOrderItems = orderItemsRepository.saveAll(orderItems);
        return savedOrderItems;
    }

}