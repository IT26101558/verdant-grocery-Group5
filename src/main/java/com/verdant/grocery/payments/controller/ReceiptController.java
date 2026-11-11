package com.verdant.grocery.payments.controller;
import com.verdant.grocery.payments.service.PaymentService;
import com.verdant.grocery.orders.service.OrderService;
import com.verdant.grocery.security.SecurityGate;
import com.verdant.grocery.common.ApiException;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/payments")public class ReceiptController {
 private final PaymentService payments;private final OrderService orders;
 public ReceiptController(PaymentService payments,OrderService orders){this.payments=payments;this.orders=orders;}
 @GetMapping("/mine")public Object mine(HttpSession s){String id=SecurityGate.user(s);return payments.all().stream().filter(p->p.getCustomerId().equals(id)&&!p.getArchived()).toList();}
 @GetMapping("/receipt/{id}")public Object receipt(@PathVariable String id,HttpSession s){var p=payments.get(id);if(!p.getCustomerId().equals(SecurityGate.user(s))||p.getArchived())throw ApiException.denied();return Map.of("payment",p,"order",orders.get(p.getOrderId()),"store","Verdant Grocery");}
}
