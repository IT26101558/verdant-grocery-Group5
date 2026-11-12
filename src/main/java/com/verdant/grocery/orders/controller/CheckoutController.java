package com.verdant.grocery.orders.controller;
import com.verdant.grocery.orders.service.CheckoutService;
import com.verdant.grocery.security.SecurityGate;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/checkout")public class CheckoutController {
 private final CheckoutService checkout;
 public CheckoutController(CheckoutService checkout){this.checkout=checkout;}
 public record Place(String method,String key){}
 @GetMapping("/quote")public Object quote(HttpSession s){return checkout.quote(SecurityGate.user(s));}
 @PostMapping("/place")public Object place(@RequestBody Place p,HttpSession s){return checkout.place(SecurityGate.user(s),p.method(),p.key());}
 @PostMapping("/demo/{id}/approve")public Object approve(@PathVariable String id,HttpSession s){return checkout.approveDemo(SecurityGate.user(s),id);}
 @PostMapping("/demo/{id}/decline")public Object decline(@PathVariable String id,HttpSession s){return checkout.declineDemo(SecurityGate.user(s),id);}
}
