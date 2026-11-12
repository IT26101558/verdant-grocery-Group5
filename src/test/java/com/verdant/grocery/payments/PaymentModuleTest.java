package com.verdant.grocery.payments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.math.BigDecimal;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest class PaymentModuleTest {
 static final Path folder;
 static{try{folder=Files.createTempDirectory("verdant-payments-");}catch(Exception e){throw new RuntimeException(e);}}
 @DynamicPropertySource static void properties(DynamicPropertyRegistry r){r.add("app.data-dir",folder::toString);}

 @Autowired com.verdant.grocery.customers.service.CustomerService customers;
 @Autowired com.verdant.grocery.products.service.ProductService products;
 @Autowired com.verdant.grocery.orders.service.CheckoutService checkout;
 @Autowired com.verdant.grocery.payments.service.PaymentService payments;
 @Test void cashAndDemoCardStatus(){
   var c=customers.register("Demo Buyer",UUID.randomUUID()+"@example.com","0771234567","Colombo","testpass123","testpass123");
   var o=checkout.manualCod(c.getId(),products.active().get(0).getId(),1);
   var payment=payments.forOrder(o.getId()).orElseThrow();assertEquals("PENDING",payment.getStatus());
   var edit=new com.verdant.grocery.payments.model.Payment();edit.setStatus("PAID");payments.update(payment.getId(),edit);
   assertEquals("PAID",payments.get(payment.getId()).getStatus());
   assertThrows(com.verdant.grocery.common.ApiException.class,()->payments.delete(payment.getId()));
 }

}
