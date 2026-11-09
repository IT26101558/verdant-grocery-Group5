package com.verdant.grocery.orders;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.math.BigDecimal;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest class OrderModuleTest {
 static final Path folder;
 static{try{folder=Files.createTempDirectory("verdant-orders-");}catch(Exception e){throw new RuntimeException(e);}}
 @DynamicPropertySource static void properties(DynamicPropertyRegistry r){r.add("app.data-dir",folder::toString);}

 @Autowired com.verdant.grocery.customers.service.CustomerService customers;
 @Autowired com.verdant.grocery.products.service.ProductService products;
 @Autowired com.verdant.grocery.inventory.service.InventoryService stock;
 @Autowired com.verdant.grocery.orders.service.CartService carts;
 @Autowired com.verdant.grocery.orders.service.CheckoutService checkout;
 @Autowired com.verdant.grocery.orders.service.OrderService orders;
 @Test void cartAndPendingCheckout(){
   var c=customers.register("Demo Buyer",UUID.randomUUID()+"@example.com","0771234567","Kandy","testpass123","testpass123");
   var p=products.active().get(0);carts.put(c.getId(),p.getId(),1);assertEquals(1,carts.get(c.getId()).getItems().size());
   var o=checkout.place(c.getId(),"DEMO_CARD",UUID.randomUUID().toString());assertEquals("PENDING_PAYMENT",o.getStatus());
   checkout.declineDemo(c.getId(),o.getId());assertEquals("CANCELLED",orders.get(o.getId()).getStatus());checkout.cancelAdmin(o.getId());
 }

}
