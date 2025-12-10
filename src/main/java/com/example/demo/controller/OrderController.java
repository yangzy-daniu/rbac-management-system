package com.example.demo.controller;

import com.example.demo.dto.CreateOrderRequest;
import com.example.demo.dto.OrderDTO;
import com.example.demo.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // 获取订单列表
    @GetMapping("/getOrdersByPage")
    public Page<OrderDTO> getOrdersByPage(
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String customer,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return orderService.getOrders(page, size, orderNo, customer, status);
    }

    // 获取订单详情
    @GetMapping("/getOrderDetail/{id}")
    public ResponseEntity<OrderDTO> getOrderDetail(@PathVariable Long id) {
        OrderDTO order = orderService.getOrderById(id);
        return ResponseEntity.ok(order);
    }

    // 创建订单
    @PostMapping("/createOrder")
    public ResponseEntity<OrderDTO> createOrder(@RequestBody CreateOrderRequest request) {
        OrderDTO order = orderService.createOrder(request);
        return ResponseEntity.ok(order);
    }

    // 更新订单状态
    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam String operator) {
        orderService.updateOrderStatus(id, status, operator);
        return ResponseEntity.ok().build();
    }

    // 更新订单
    @PutMapping("/{id}")
    public ResponseEntity<OrderDTO> updateOrder(
            @PathVariable Long id,
            @RequestBody CreateOrderRequest request) {
        OrderDTO updatedOrder = orderService.updateOrder(id, request);
        return ResponseEntity.ok(updatedOrder);
    }

    // 删除订单
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.ok().build();
    }
}