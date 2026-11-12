package com.verdant.grocery.orders.controller;
import com.verdant.grocery.orders.service.CartService;
import com.verdant.grocery.security.SecurityGate;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/cart")public class CartController {
 private final CartService carts;
 public CartController(CartService carts){this.carts=carts;}
 public record Item(String productId,int quantity){}
 @GetMapping public Object get(HttpSession s){return carts.get(SecurityGate.user(s));}
 @PostMapping("/items")public Object add(@RequestBody Item i,HttpSession s){if(i.quantity()<1)throw com.verdant.grocery.common.ApiException.bad("Quantity must be at least one");String u=SecurityGate.user(s);int qty=carts.get(u).getItems().stream().filter(x->x.getProductId().equals(i.productId())).mapToInt(x->x.getQuantity()).sum();return carts.put(u,i.productId(),qty+i.quantity());}
 @PutMapping("/items")public Object change(@RequestBody Item i,HttpSession s){return carts.put(SecurityGate.user(s),i.productId(),i.quantity());}
 @DeleteMapping("/items/{id}")public Object remove(@PathVariable String id,HttpSession s){return carts.put(SecurityGate.user(s),id,0);}
}
