package com.shop.service;

import com.shop.dto.OrderDto;
import com.shop.dto.OrderHistDto;
import com.shop.dto.OrderItemDto;
import com.shop.entity.*;
import com.shop.repository.ItemImgRepository;
import com.shop.repository.ItemRepository;
import com.shop.repository.MemberRepository;
import com.shop.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderService { // 주문서비스
    private final ItemRepository itemRepository;
    private final MemberRepository memberRepository;
    private final OrderRepository orderRepository;
    private final ItemImgRepository itemImgRepository;

    public Long order(OrderDto orderDto, String email){
        //빌더패턴
        //item 엔티티 객체 얻기
        Item item = itemRepository.findById(orderDto.getItemId())
                .orElseThrow(EntityNotFoundException::new);
        //member 엔티티 객체 얻기
        Member member = memberRepository.findByEmail(email);

        //주문아이템 리스트 객체 생성
        List<OrderItem> orderItemList = new ArrayList<>();
        OrderItem orderItem = OrderItem.createOrderItem(item, orderDto.getCount());
        orderItemList.add(orderItem);

        //OrderDto -> Order
        Order order = Order.createOrder(member, orderItemList);
        orderRepository.save(order); // DB에 Order 추가 -> OrderItem도 추가됨
        return order.getId();
    }

    @Transactional(readOnly = true)
    public Page<OrderHistDto> getOrderList(String email, Pageable pageable){
        List<Order> orders = orderRepository.findOrders(email, pageable);
        Long totalCount = orderRepository.countOrder(email);
        List<OrderHistDto> orderHistDtos = new ArrayList<>();
        //Order -> OrderHistDto
        //OrderItem -> OrderItemDto
        for(Order order : orders){
            OrderHistDto orderHistDto = new OrderHistDto(order);
            List<OrderItem> orderItems = order.getOrderItems();
            for(OrderItem orderItem : orderItems){
                ItemImg itemImg = itemImgRepository.findByItemIdAndRepImgYn(orderItem.getItem().getId(),
                        "Y");
                OrderItemDto orderItemDto = new OrderItemDto(orderItem, itemImg.getImgUrl());
                orderHistDto.addOrderItemDto(orderItemDto);
            }

            orderHistDtos.add(orderHistDto);
        }
        return new PageImpl<OrderHistDto>(orderHistDtos, pageable, totalCount);
    }

    @Transactional(readOnly = true)
    public boolean validateOrder(Long orderId, String email) {
        // 이메일로 현재 사용자를 조회
        Member curMember = memberRepository.findByEmail(email);

        // 주문 ID로 주문을 조회하고, 주문이 존재하지 않으면 예외를 던짐
        Order order = orderRepository.findById(orderId).orElseThrow(EntityNotFoundException::new);

        // 주문에서 저장된 회원 정보를 가져옴
        Member savedMember = order.getMember();

        // 현재 사용자의 이메일과 주문에 저장된 회원의 이메일이 같은지 비교
        if (!StringUtils.equals(curMember.getEmail(), savedMember.getEmail())) {
            return false;
        }

        return true;
    }

    public void cancelOrder(Long orderId) {
        // 주문 ID를 기반으로 주문을 조회하고, 주문이 존재하지 않으면 EntityNotFoundException 예외를 던짐
        Order order = orderRepository.findById(orderId).orElseThrow(EntityNotFoundException::new);

        // 조회된 주문을 취소
        order.cancelOrder();
    }


}
