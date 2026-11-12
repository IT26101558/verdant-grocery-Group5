package com.verdant.grocery.customers.controller;
import com.verdant.grocery.customers.service.CustomerService;
import com.verdant.grocery.security.SecurityGate;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/account") public class AccountController {
  private final CustomerService customers;
  public AccountController(CustomerService customers){this.customers=customers;}
  @GetMapping("/profile")public Object profile(HttpSession s){return customers.safe(customers.get(SecurityGate.user(s)));}
  public record Profile(String fullName,String email,String mobile,String address){}
  @PutMapping("/profile")public Object update(@RequestBody Profile p,HttpSession s){return customers.safe(customers.modifyProfile(SecurityGate.user(s),p.fullName(),p.email(),p.mobile(),p.address()));}
}
