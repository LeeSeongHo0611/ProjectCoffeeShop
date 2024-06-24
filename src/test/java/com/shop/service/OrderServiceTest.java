package com.shop.service;

import com.shop.constant.OrderStatus;
import com.shop.entity.Item;
import com.shop.entity.Member;
import com.shop.entity.Order;
import com.shop.entity.OrderItem;
import com.shop.repository.ItemRepository;
import com.shop.repository.MemberRepository;
import com.shop.repository.OrderRepository;
import com.shop.dto.OrderDto;
import com.shop.constant.ItemSellStatus;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
@TestPropertySource(locations = "classpath:application-test.properties")
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Item savedItem;

    @BeforeEach
    void setUp() {
        savedItem = saveItem();
    }

    public Item saveItem() {
        Item item = new Item();
        item.setItemNm("테스트 상품");
        item.setPrice(10000);
        item.setItemDetail("테스트 상품 상세 설명");
        item.setItemSellStatus(ItemSellStatus.SELL);
        item.setStockNumber(100);
        return itemRepository.save(item);
    }

    public Member saveMember(String email) {
        Member member = new Member();
        member.setEmail(email);
        // 추가적인 멤버 정보 설정
        return memberRepository.save(member);
    }

    @Test
    @DisplayName("주문 테스트")
    public void order() {
        String uniqueEmail = "test" + UUID.randomUUID() + "@test.com";
        Member member = saveMember(uniqueEmail);

        OrderDto orderDto = new OrderDto();
        orderDto.setCount(10);
        orderDto.setItemId(savedItem.getId());

        Long orderId = orderService.order(orderDto, member.getEmail());
        Order order = orderRepository.findById(orderId)
                .orElseThrow(EntityNotFoundException::new);
        List<OrderItem> orderItems = order.getOrderItems();

        int totalPrice = orderDto.getCount() * savedItem.getPrice();
        assertEquals(totalPrice, order.getTotalPrice());
    }



    // 저장하는 메서드 예시. 실제 코드에 맞게 구현 필요
    private Member saveMember() {
        // 멤버를 저장하고 반환하는 로직을 작성하세요.
        Member member = new Member();
        member.setEmail("test@example.com");
        member.setName("Test User");
        return memberRepository.save(member); // 실제 저장 로직 추가
    }

    @Test
    @DisplayName("주문 취소 테스트")
    public void cancelOrder() {
        // 테스트 데이터를 저장
        Item item = saveItem();
        Member member = saveMember();

        // 주문 DTO 생성 및 설정
        OrderDto orderDto = new OrderDto();
        orderDto.setCount(10);
        orderDto.setItemId(item.getId());

        // 주문 생성
        Long orderId = orderService.order(orderDto, member.getEmail());

        // 주문 조회
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));

        // 주문 취소
        orderService.cancelOrder(orderId);

        // 주문 상태 확인
        assertEquals(OrderStatus.CANCEL, order.getOrderStatus());

        // 주문 취소 후 재고 수량 검증
        assertEquals(100, item.getStockNumber()); // 여기서 100은 예시 값입니다. 실제로 재고 수량을 관리하는 로직에 따라 값이 달라질 수 있습니다.
    }
}
