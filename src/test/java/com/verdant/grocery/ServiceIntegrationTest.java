package com.verdant.grocery;
import com.verdant.grocery.customers.service.*;
import com.verdant.grocery.products.service.ProductService;
import com.verdant.grocery.inventory.service.InventoryService;
import com.verdant.grocery.orders.service.*;
import com.verdant.grocery.deliveries.service.DeliveryService;
import com.verdant.grocery.payments.service.PaymentService;
import com.verdant.grocery.products.model.Product;
import com.verdant.grocery.inventory.model.Stock;
import com.verdant.grocery.deliveries.model.Delivery;
import com.verdant.grocery.payments.model.Payment;
import com.verdant.grocery.customers.model.Feedback;
import com.verdant.grocery.common.ApiException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.nio.file.*;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest public class ServiceIntegrationTest {
  private static final Path tempDir;
  static{try{tempDir=Files.createTempDirectory("verdant-test-");}catch(Exception e){throw new RuntimeException(e);}}
  @DynamicPropertySource static void properties(DynamicPropertyRegistry r){r.add("app.data-dir",tempDir::toString);}
  @Autowired CustomerService customers;
  @Autowired ProductService products;
  @Autowired InventoryService stock;
  @Autowired CartService cart;
  @Autowired OrderService orders;
  @Autowired CheckoutService checkout;
  @Autowired DeliveryService deliveries;
  @Autowired PaymentService payments;
  @Autowired FeedbackService feedback;
  private static final java.util.concurrent.atomic.AtomicInteger mobileNumber=new java.util.concurrent.atomic.AtomicInteger(1000000);
  private String customer(){return customers.register("Test Person",UUID.randomUUID()+"@example.com","077"+mobileNumber.incrementAndGet(),"Colombo, Sri Lanka","password123","password123").getId();}
  private Product product(){Product p=new Product();p.setName("Organic guava");p.setCategory("fruits");p.setDescription("Fresh local guava");p.setUnit("1 kg");p.setPrice(new BigDecimal("500.00"));p.setActive(true);return products.create(p);}
  private Stock stockFor(String id,int count){Stock s=new Stock();s.setProductId(id);s.setQuantity(count);s.setReorderLevel(2);return stock.create(s);}
  @Test void allSixDomainsHaveWorkingPersistenceOperations(){
     String uid=customer();assertEquals(uid,customers.get(uid).getId());
     var c=customers.get(uid);c.setFullName("Updated Person");customers.update(uid,c);assertEquals("Updated Person",customers.get(uid).getFullName());
     var p=product();p.setPrice(new BigDecimal("600.00"));products.update(p.getId(),p);assertEquals(0,products.get(p.getId()).getPrice().compareTo(new BigDecimal("600.00")));
     Stock s=stockFor(p.getId(),15);Stock edit=new Stock();edit.setQuantity(12);edit.setReorderLevel(3);stock.update(s.getId(),edit);assertEquals(12,stock.get(s.getId()).getQuantity());
     cart.put(uid,p.getId(),2);var order=checkout.place(uid,"COD",UUID.randomUUID().toString());
     assertEquals("CONFIRMED",orders.get(order.getId()).getStatus());assertEquals(10,stock.forProduct(p.getId()).getQuantity());
     var delivery=deliveries.forOrder(order.getId()).orElseThrow();Delivery update=new Delivery();update.setStatus("PACKED");update.setDriver("Demo driver");deliveries.update(delivery.getId(),update);assertEquals("PACKED",deliveries.get(delivery.getId()).getStatus());
     Payment payment=payments.forOrder(order.getId()).orElseThrow();Payment paid=new Payment();paid.setStatus("PAID");payments.update(payment.getId(),paid);assertEquals("PAID",payments.get(payment.getId()).getStatus());
     Feedback f=new Feedback();f.setCustomerId(uid);f.setRating(5);f.setComment("Great service");f=feedback.create(f);f.setComment("Excellent service");feedback.update(f.getId(),f);assertEquals("Excellent service",feedback.get(f.getId()).getComment());feedback.delete(f.getId());
     deliveries.delete(delivery.getId());products.delete(p.getId());assertFalse(products.get(p.getId()).getActive());
     Stock zero=new Stock();zero.setQuantity(0);zero.setReorderLevel(2);stock.update(s.getId(),zero);stock.delete(s.getId());
     // Historical orders prohibit deleting the related customer.
     assertNotNull(customers.get(uid));
  }
  @Test void demoDoesNotConfirmBeforeServerApprovalAndFailureReleasesStock(){
     String uid=customer();Product p=product();stockFor(p.getId(),8);cart.put(uid,p.getId(),2);
     var o=checkout.place(uid,"DEMO_CARD",UUID.randomUUID().toString());
     assertEquals("PENDING_PAYMENT",orders.get(o.getId()).getStatus());assertEquals(2,stock.forProduct(p.getId()).getReserved());assertTrue(deliveries.forOrder(o.getId()).isEmpty());
     checkout.declineDemo(uid,o.getId());assertEquals("CANCELLED",orders.get(o.getId()).getStatus());assertEquals(0,stock.forProduct(p.getId()).getReserved());
     var o2=checkout.place(uid,"DEMO_CARD",UUID.randomUUID().toString());checkout.approveDemo(uid,o2.getId());
     assertEquals("CONFIRMED",orders.get(o2.getId()).getStatus());assertEquals(6,stock.forProduct(p.getId()).getQuantity());assertEquals(0,stock.forProduct(p.getId()).getReserved());
     assertEquals("PAID",payments.forOrder(o2.getId()).orElseThrow().getStatus());assertTrue(deliveries.forOrder(o2.getId()).isPresent());
     var again=checkout.approveDemo(uid,o2.getId());assertEquals(o2.getId(),again.getId());
  }
  @Test void stockCannotBeOversoldAndPendingOrderCanBeDeleted(){
     String uid=customer();var p=product();stockFor(p.getId(),1);assertThrows(ApiException.class,()->cart.put(uid,p.getId(),2));cart.put(uid,p.getId(),1);
     var o=checkout.place(uid,"DEMO_CARD",UUID.randomUUID().toString());assertThrows(ApiException.class,()->checkout.place(uid,"COD",UUID.randomUUID().toString()));
     checkout.cancelAdmin(o.getId());assertThrows(ApiException.class,()->orders.get(o.getId()));assertEquals(1,stock.available(p.getId()));
  }
}
