package com.verdant.grocery.customers.controller;

import com.verdant.grocery.orders.service.OrderService;
import com.verdant.grocery.deliveries.service.DeliveryService;
import com.verdant.grocery.payments.service.PaymentService;
import com.verdant.grocery.security.SecurityGate;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.LinkedHashMap;
import java.util.Map;

/** Customer-only dashboard. Counts exclude other customers' saved records. */
@RestController
@RequestMapping("/api/customer")
public class CustomerDashboardController {
    private final OrderService orders;
    private final DeliveryService deliveries;
    private final PaymentService payments;

    public CustomerDashboardController(OrderService orders, DeliveryService deliveries, PaymentService payments) {
        this.orders=orders;
        this.deliveries=deliveries;
        this.payments=payments;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(HttpSession session) {
        String id=SecurityGate.user(session);
        var myOrders=orders.all().stream().filter(o -> id.equals(o.getCustomerId())).toList();
        var myDeliveries=deliveries.all().stream().filter(d -> id.equals(d.getCustomerId())).toList();
        var myPayments=payments.all().stream().filter(p -> id.equals(p.getCustomerId()) && !p.getArchived()).toList();
        Map<String,Object> stats=new LinkedHashMap<>();
        stats.put("orders",myOrders.size());
        stats.put("activeDeliveries",myDeliveries.stream().filter(d -> !"DELIVERED".equals(d.getStatus())).count());
        stats.put("payments",myPayments.size());
        stats.put("pendingPayments",myPayments.stream().filter(p -> "PENDING".equals(p.getStatus())).count());
        stats.put("recentOrders",myOrders.stream().sorted(java.util.Comparator.comparing(com.verdant.grocery.orders.model.Order::getCreatedAt, java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder())).reversed()).limit(3).toList());
        return stats;
    }
}
