package com.verdant.grocery.orders.controller;
import com.verdant.grocery.orders.service.OrderService;
import com.verdant.grocery.security.SecurityGate;
import com.verdant.grocery.common.ApiException;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/orders")public class OrderController {
 private final OrderService orders;
 public OrderController(OrderService orders){this.orders=orders;}
 @GetMapping public Object mine(HttpSession s){String id=SecurityGate.user(s);return orders.all().stream().filter(o->o.getCustomerId().equals(id)).toList();}
 @GetMapping("/{id}")public Object one(@PathVariable String id,HttpSession s){var o=orders.get(id);if(!o.getCustomerId().equals(SecurityGate.user(s)))throw ApiException.denied();return o;}
}
