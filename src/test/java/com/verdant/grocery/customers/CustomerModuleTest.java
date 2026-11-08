package com.verdant.grocery.customers;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.math.BigDecimal;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest class CustomerModuleTest {
 static final Path folder;
 static{try{folder=Files.createTempDirectory("verdant-customers-");}catch(Exception e){throw new RuntimeException(e);}}
 @DynamicPropertySource static void properties(DynamicPropertyRegistry r){r.add("app.data-dir",folder::toString);}

 @Autowired com.verdant.grocery.customers.service.CustomerService customers;
 @Autowired com.verdant.grocery.customers.service.FeedbackService feedback;
 @Test void accountAndFeedback() {
   String email=UUID.randomUUID()+"@example.com";
   var c=customers.register("Demo Buyer",email,"+94771234567","Jaffna","pass123456","pass123456");
   assertEquals("0771234567",c.getMobile());
   c.setFullName("Updated Buyer");customers.update(c.getId(),c);
   assertEquals("Updated Buyer",customers.get(c.getId()).getFullName());
   var f=new com.verdant.grocery.customers.model.Feedback();f.setCustomerId(c.getId());f.setRating(5);f.setComment("Wonderful");f=feedback.create(f);
   f.setComment("Very good");feedback.update(f.getId(),f);feedback.delete(f.getId());
   customers.delete(c.getId());assertTrue(customers.findEmail(email).isEmpty());
 }

}
