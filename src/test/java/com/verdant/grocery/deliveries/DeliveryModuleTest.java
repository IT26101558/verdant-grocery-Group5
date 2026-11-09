package com.verdant.grocery.deliveries;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.math.BigDecimal;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest class DeliveryModuleTest {
 static final Path folder;
 static{try{folder=Files.createTempDirectory("verdant-deliveries-");}catch(Exception e){throw new RuntimeException(e);}}
 @DynamicPropertySource static void properties(DynamicPropertyRegistry r){r.add("app.data-dir",folder::toString);}

 @Autowired com.verdant.grocery.customers.service.CustomerService customers;
 @Autowired com.verdant.grocery.products.service.ProductService products;
 @Autowired com.verdant.grocery.orders.service.CheckoutService checkout;
 @Autowired com.verdant.grocery.deliveries.service.DeliveryService deliveries;
 @Test void deliveryCrudAndTracking(){
   var c=customers.register("Demo Buyer",UUID.randomUUID()+"@example.com","0771234567","Jaffna","testpass123","testpass123");
   var o=checkout.manualCod(c.getId(),products.active().get(0).getId(),1);
   var d=deliveries.forOrder(o.getId()).orElseThrow();assertEquals("PROCESSING",d.getStatus());
   var edit=new com.verdant.grocery.deliveries.model.Delivery();edit.setStatus("IN_TRANSIT");edit.setDriver("Courier");deliveries.update(d.getId(),edit);assertEquals("IN_TRANSIT",deliveries.get(d.getId()).getStatus());deliveries.delete(d.getId());
   var newDelivery=deliveries.createFor(o);assertNotNull(newDelivery.getTrackingCode());
 }

}
