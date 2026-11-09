package com.verdant.grocery.deliveries.controller;
import com.verdant.grocery.deliveries.service.DeliveryService;
import com.verdant.grocery.security.SecurityGate;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/deliveries")public class TrackingController {
 private final DeliveryService deliveries;
 public TrackingController(DeliveryService deliveries){this.deliveries=deliveries;}
 @GetMapping("/mine")public Object mine(HttpSession s){String id=SecurityGate.user(s);return deliveries.all().stream().filter(d->d.getCustomerId().equals(id)).toList();}
}
