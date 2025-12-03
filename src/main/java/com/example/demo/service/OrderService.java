package com.example.demo.service;

import com.example.demo.dto.CreateOrderRequest;
import com.example.demo.dto.OrderDTO;
import com.example.demo.dto.OrderItemDTO;
import com.example.demo.dto.OrderLogDTO;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import com.example.demo.entity.OrderLog;
import com.example.demo.repository.OrderItemRepository;
import com.example.demo.repository.OrderLogRepository;
import com.example.demo.repository.OrderRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderLogRepository orderLogRepository;
    private final SystemTodoService systemTodoService;

    // 查询订单列表（分页+搜索）
    public Page<OrderDTO> getOrders(int page, int size, String orderNo, String customer, String status) {
        Specification<Order> spec = buildOrderSpecification(orderNo, customer, status);
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Order> orders = orderRepository.findAll(spec, pageable);
        return orders.map(this::convertToDTO);
    }

    // 构建订单查询条件
    private Specification<Order> buildOrderSpecification(String orderNo, String customer, String status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(orderNo)) {
                predicates.add(cb.like(root.get("orderNo"), "%" + orderNo + "%"));
            }

            if (StringUtils.hasText(customer)) {
                predicates.add(cb.like(root.get("customer"), "%" + customer + "%"));
            }

            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            query.orderBy(cb.desc(root.get("createTime")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // 根据ID获取订单详情
    public OrderDTO getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
        return convertToDetailDTO(order);
    }

    // 创建订单
    @Transactional
    public OrderDTO createOrder(CreateOrderRequest request) {
        // 生成订单号
        String orderNo = generateOrderNo();

        // 计算总金额
        BigDecimal totalAmount = request.getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 创建订单
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setCustomer(request.getCustomer());
        order.setAmount(totalAmount);
        order.setProductCount(request.getItems().size());
        order.setRemark(request.getRemark());
        order.setStatus("pending");

        Order savedOrder = orderRepository.save(order);

        // 保存订单商品项
        List<OrderItem> items = request.getItems().stream()
                .map(itemRequest -> {
                    OrderItem item = new OrderItem();
                    item.setOrder(savedOrder);
                    item.setProductName(itemRequest.getProductName());
                    item.setPrice(itemRequest.getPrice());
                    item.setQuantity(itemRequest.getQuantity());
                    item.setDescription(itemRequest.getDescription());
                    return item;
                })
                .collect(Collectors.toList());
        orderItemRepository.saveAll(items);

        // 记录操作日志
        addOrderLog(savedOrder.getId(), "创建订单", "系统", "info", "用户提交订单");
        // 自动创建订单处理待办
        try {
            systemTodoService.createOrderProcessingTodo(
                    savedOrder.getOrderNo(),
                    savedOrder.getItems().size()
            );
        } catch (Exception e) {
            log.error("创建订单处理待办失败，但不影响订单创建: {}", e.getMessage());
        }
        return convertToDetailDTO(savedOrder);
    }

    // 更新订单状态
    @Transactional
    public void updateOrderStatus(Long orderId, String status, String operator) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        String oldStatus = order.getStatus();
        order.setStatus(status);
        orderRepository.save(order);

        // 记录状态变更日志
        String action = getStatusAction(status);
        addOrderLog(orderId, action, operator, "success",
                String.format("订单状态从 %s 变更为 %s", getStatusText(oldStatus), getStatusText(status)));
    }

    // 更新订单（客户、备注、商品项）
    @Transactional
    public OrderDTO updateOrder(Long id, CreateOrderRequest request) {
        // 1. 校验订单是否存在
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        // 2. 更新主表字段
        order.setCustomer(request.getCustomer());
        order.setRemark(request.getRemark());

        // 3. 重新计算总金额与商品数量
        BigDecimal newAmount = request.getItems().stream()
                .map(item -> item.getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setAmount(newAmount);
        order.setProductCount(request.getItems().size());

        // 4. 级联删除旧订单项（JPA 会自动处理）
        orderItemRepository.deleteByOrderId(order.getId());

        // 5. 插入新订单项
        List<OrderItem> newItems = request.getItems().stream()
                .map(itemReq -> {
                    OrderItem item = new OrderItem();
                    item.setOrder(order);
                    item.setProductName(itemReq.getProductName());
                    item.setPrice(itemReq.getPrice());
                    item.setQuantity(itemReq.getQuantity());
                    item.setDescription(itemReq.getDescription());
                    return item;
                })
                .collect(Collectors.toList());
        orderItemRepository.saveAll(newItems);

        // 6. 记录操作日志
        addOrderLog(order.getId(), "编辑订单", "系统", "info", "用户修改订单内容与商品项");

        // 7. 保存并返回最新 DTO
        Order saved = orderRepository.save(order);
        return convertToDetailDTO(saved);
    }

    // 删除订单
    @Transactional
    public void deleteOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        // 由于配置了级联删除，只需要删除订单即可
        orderRepository.delete(order);
    }

    // 添加订单操作日志
    private void addOrderLog(Long orderId, String action, String operator, String type, String remark) {
        OrderLog log = new OrderLog();
        log.setOrder(orderRepository.getReferenceById(orderId));
        log.setAction(action);
        log.setOperator(operator);
        log.setType(type);
        log.setRemark(remark);
        orderLogRepository.save(log);
    }

    // 生成订单号
    private String generateOrderNo() {
        String dateStr = LocalDate.now().format(DATE_FORMATTER);

        // 生成4位序列号（0001-9999）
        String sequence = String.format("%04d", (int)((Math.random() * 9999) + 1));

        return "ORD" + dateStr + sequence;
    }

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    // 转换实体为DTO（列表用）
    private OrderDTO convertToDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setOrderNo(order.getOrderNo());
        dto.setCustomer(order.getCustomer());
        dto.setAmount(order.getAmount());
        dto.setStatus(order.getStatus());
        dto.setCreateTime(order.getCreateTime());
        dto.setProductCount(order.getProductCount());
        dto.setRemark(order.getRemark());
        return dto;
    }

    // 转换实体为详细DTO（详情用）
    private OrderDTO convertToDetailDTO(Order order) {
        OrderDTO dto = convertToDTO(order);

        // 获取订单商品项
        List<OrderItemDTO> itemDTOs = orderItemRepository.findByOrderId(order.getId()).stream()
                .map(item -> {
                    OrderItemDTO itemDTO = new OrderItemDTO();
                    itemDTO.setId(item.getId());
                    itemDTO.setProductName(item.getProductName());
                    itemDTO.setPrice(item.getPrice());
                    itemDTO.setQuantity(item.getQuantity());
                    itemDTO.setDescription(item.getDescription());
                    itemDTO.setTotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                    return itemDTO;
                })
                .collect(Collectors.toList());
        dto.setItems(itemDTOs);

        // 获取操作日志
        List<OrderLogDTO> logDTOs = orderLogRepository.findByOrderIdOrderByCreateTimeDesc(order.getId()).stream()
                .map(log -> {
                    OrderLogDTO logDTO = new OrderLogDTO();
                    logDTO.setId(log.getId());
                    logDTO.setAction(log.getAction());
                    logDTO.setOperator(log.getOperator());
                    logDTO.setType(log.getType());
                    logDTO.setRemark(log.getRemark());
                    logDTO.setCreateTime(log.getCreateTime());
                    return logDTO;
                })
                .collect(Collectors.toList());
        dto.setLogs(logDTOs);

        return dto;
    }

    private String getStatusAction(String status) {
        switch (status) {
            case "paid": return "支付成功";
            case "shipped": return "订单发货";
            case "completed": return "订单完成";
            case "cancelled": return "订单取消";
            default: return "状态更新";
        }
    }

    private String getStatusText(String status) {
        switch (status) {
            case "pending": return "待支付";
            case "paid": return "已支付";
            case "shipped": return "已发货";
            case "completed": return "已完成";
            case "cancelled": return "已取消";
            default: return "未知";
        }
    }
}