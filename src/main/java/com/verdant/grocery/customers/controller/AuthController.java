package com.verdant.grocery.customers.controller;
import com.verdant.grocery.customers.service.CustomerService;
import com.verdant.grocery.security.SecurityGate;
import com.verdant.grocery.common.*;
import jakarta.servlet.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.*;
// Owner: Vakeesan. Separate user registration / login / admin login.
@RestController @RequestMapping("/api/auth") public class AuthController {
 private final CustomerService customers;
 public AuthController(CustomerService customers){this.customers=customers;}
 public record Registration(String fullName,String email,String mobile,String address,String password,String confirmPassword){}
 public record Login(String email,String password){}
 @PostMapping("/register")public Object register(@RequestBody Registration r){return customers.safe(customers.register(r.fullName(),r.email(),r.mobile(),r.address(),r.password(),r.confirmPassword()));}
 @PostMapping("/login")public Object login(@RequestBody Login r,HttpServletRequest request){return sign(r,request,"CUSTOMER");}
 @PostMapping("/admin-login")public Object adminLogin(@RequestBody Login r,HttpServletRequest request){return sign(r,request,"ADMIN");}
 private Object sign(Login r,HttpServletRequest request,String role){
   var c=customers.findEmail(Checks.email(r.email())).orElseThrow(()->new ApiException(HttpStatus.UNAUTHORIZED,"Incorrect credentials"));
   if(!role.equals(c.getRole())||!customers.authenticate(c,r.password()))throw new ApiException(HttpStatus.UNAUTHORIZED,"Incorrect credentials");
   request.getSession(true);request.changeSessionId();HttpSession session=request.getSession();session.setAttribute("customerId",c.getId());session.setAttribute("role",role);return customers.safe(c);
 }
 @GetMapping("/me")public Object me(HttpServletRequest request){
   HttpSession session=request.getSession(false);
   if(session==null||session.getAttribute("customerId")==null)return Map.of("authenticated",false);
   try{
     var account=customers.get(session.getAttribute("customerId").toString());
     if(!account.getActive()||!account.getRole().equals(session.getAttribute("role"))){session.invalidate();return Map.of("authenticated",false);}
     return customers.safe(account);
   }catch(ApiException ex){session.invalidate();return Map.of("authenticated",false);}
 }
 @PostMapping("/logout")public Object logout(HttpSession session){session.invalidate();return Map.of("ok",true);}
}
