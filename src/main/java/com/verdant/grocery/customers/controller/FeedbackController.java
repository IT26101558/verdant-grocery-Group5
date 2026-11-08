package com.verdant.grocery.customers.controller;
import com.verdant.grocery.customers.service.FeedbackService;
import com.verdant.grocery.customers.model.Feedback;
import com.verdant.grocery.security.SecurityGate;
import com.verdant.grocery.common.ApiException;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/feedback")public class FeedbackController {
 private final FeedbackService feedback;
 public FeedbackController(FeedbackService feedback){this.feedback=feedback;}
 @GetMapping public Object mine(HttpSession s){String id=SecurityGate.user(s);return feedback.all().stream().filter(f->f.getCustomerId().equals(id)).toList();}
 @PostMapping public Object create(@RequestBody Feedback f,HttpSession s){f.setCustomerId(SecurityGate.user(s));f.setReply("");return feedback.create(f);}
 @PutMapping("/{id}")public Object update(@PathVariable String id,@RequestBody Feedback f,HttpSession s){Feedback old=feedback.get(id);if(!old.getCustomerId().equals(SecurityGate.user(s)))throw ApiException.denied();f.setCustomerId(old.getCustomerId());f.setReply(old.getReply());return feedback.update(id,f);}
 @DeleteMapping("/{id}")public Object delete(@PathVariable String id,HttpSession s){if(!feedback.get(id).getCustomerId().equals(SecurityGate.user(s)))throw ApiException.denied();feedback.delete(id);return java.util.Map.of("ok",true);}
}
