package com.verdant.grocery.admin;
import com.verdant.grocery.common.*;
import com.verdant.grocery.security.SecurityGate;
import com.verdant.grocery.customers.service.*;
import com.verdant.grocery.products.service.ProductService;
import com.verdant.grocery.inventory.service.InventoryService;
import com.verdant.grocery.orders.service.*;
import com.verdant.grocery.deliveries.service.DeliveryService;
import com.verdant.grocery.payments.service.PaymentService;
import com.verdant.grocery.customers.model.*;
import com.verdant.grocery.products.model.Product;
import com.verdant.grocery.inventory.model.Stock;
import com.verdant.grocery.orders.model.Order;
import com.verdant.grocery.deliveries.model.Delivery;
import com.verdant.grocery.payments.model.Payment;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
import java.util.*;
/**
 * Integration owner: shared admin API exposing exactly six business areas.
 * Business rules remain in each member's service, not in the UI or admin controller.
 * All /api/admin/* requests must pass the SecurityGate role check.
 */
@RestController @RequestMapping("/api/admin") public class AdminController {
  private final CustomerService customers;private final FeedbackService feedback;private final ProductService products;private final InventoryService inventory;
  private final OrderService orders;private final CheckoutService checkout;private final DeliveryService deliveries;private final PaymentService payments;
  public AdminController(CustomerService customers,FeedbackService feedback,ProductService products,InventoryService inventory,OrderService orders,CheckoutService checkout,DeliveryService deliveries,PaymentService payments){
    this.customers=customers;this.feedback=feedback;this.products=products;this.inventory=inventory;this.orders=orders;this.checkout=checkout;this.deliveries=deliveries;this.payments=payments;
  }
  @GetMapping("/overview") public Object stats(){return Map.of("customers",customers.all().stream().filter(c->"CUSTOMER".equals(c.getRole())).count(),"products",products.active().size(),"inventory",inventory.all().size(),"orders",orders.all().size(),"deliveries",deliveries.all().size(),"payments",payments.all().stream().filter(p->!p.getArchived()).count(),"lowStock",inventory.low().size());}
  @GetMapping("/customers")public Object customers(){return customers.all().stream().filter(c->"CUSTOMER".equals(c.getRole())).map(customers::safe).toList();}
  @GetMapping("/customers/{id}")public Object customer(@PathVariable String id){Customer c=customers.get(id);if(!"CUSTOMER".equals(c.getRole()))throw ApiException.denied();return customers.safe(c);}
  public record CreateCustomer(String fullName,String email,String mobile,String address,String password){}
  @PostMapping("/customers")public Object addCustomer(@RequestBody CreateCustomer c){return customers.safe(customers.register(c.fullName(),c.email(),c.mobile(),c.address(),c.password(),c.password()));}
  @PutMapping("/customers/{id}")public Object changeCustomer(@PathVariable String id,@RequestBody Customer c){return customers.safe(customers.update(id,c));}
  @DeleteMapping("/customers/{id}")public Object removeCustomer(@PathVariable String id){if(orders.all().stream().anyMatch(o->o.getCustomerId().equals(id)))throw ApiException.conflict("Customer has order history; cannot delete");if("ADMIN".equals(customers.get(id).getRole()))throw ApiException.denied();customers.delete(id);return Map.of("ok",true);}
  @GetMapping("/customers/feedback")public Object feedback(){return feedback.all();}
  @PutMapping("/customers/feedback/{id}")public Object reply(@PathVariable String id,@RequestBody Map<String,String> m){Feedback f=feedback.get(id);f.setReply(Checks.required(m.get("reply"),"Reply"));return feedback.update(id,f);}
  @GetMapping("/products")public Object products(){return products.all();}
  @GetMapping("/products/{id}")public Object product(@PathVariable String id){return products.get(id);}
  @PostMapping("/products")public Object addProduct(@RequestBody Product p){p.setActive(true);return products.create(p);}
  @PutMapping("/products/{id}")public Object changeProduct(@PathVariable String id,@RequestBody Product p){p.setActive(products.get(id).getActive());return products.update(id,p);}
  @DeleteMapping("/products/{id}")public Object removeProduct(@PathVariable String id){products.delete(id);return Map.of("ok",true);}
  @GetMapping("/inventory")public Object inventory(){return inventory.all();}
  @GetMapping("/inventory/low")public Object low(){return inventory.low();}
  @GetMapping("/inventory/{id}")public Object stock(@PathVariable String id){return inventory.get(id);}
  @PostMapping("/inventory")public Object addStock(@RequestBody Stock s){return inventory.create(s);}
  @PutMapping("/inventory/{id}")public Object changeStock(@PathVariable String id,@RequestBody Stock s){return inventory.update(id,s);}
  @DeleteMapping("/inventory/{id}")public Object removeStock(@PathVariable String id){inventory.delete(id);return Map.of("ok",true);}
  @GetMapping("/orders")public Object orders(){return orders.all();}
  @GetMapping("/orders/{id}")public Object order(@PathVariable String id){return orders.get(id);}
  public record CreateOrder(String customerId,String productId,int quantity){}
  @PostMapping("/orders")public Object addOrder(@RequestBody CreateOrder o){if(!"CUSTOMER".equals(customers.get(o.customerId()).getRole()))throw ApiException.bad("Select customer");return checkout.manualCod(o.customerId(),o.productId(),o.quantity());}
  @PutMapping("/orders/{id}")public Object changeOrder(@PathVariable String id,@RequestBody Order o){return orders.update(id,o);}
  @DeleteMapping("/orders/{id}")public Object removeOrder(@PathVariable String id){checkout.cancelAdmin(id);return Map.of("ok",true);}
  @GetMapping("/deliveries")public Object deliveries(){return deliveries.all();}
  @GetMapping("/deliveries/{id}")public Object delivery(@PathVariable String id){return deliveries.get(id);}
  @PostMapping("/deliveries")public Object addDelivery(@RequestBody Delivery d){var o=orders.get(d.getOrderId());d.setCustomerId(o.getCustomerId());return deliveries.create(d);}
  @PutMapping("/deliveries/{id}")public Object changeDelivery(@PathVariable String id,@RequestBody Delivery d){return deliveries.update(id,d);}
  @DeleteMapping("/deliveries/{id}")public Object removeDelivery(@PathVariable String id){deliveries.delete(id);return Map.of("ok",true);}
  @GetMapping("/payments")public Object payments(){return payments.all();}
  @GetMapping("/payments/{id}")public Object payment(@PathVariable String id){return payments.get(id);}
  @PostMapping("/payments")public Object addPayment(@RequestBody Payment request){
    var order=orders.get(Checks.required(request.getOrderId(),"Order"));
    if(!"CONFIRMED".equals(order.getStatus())||!"COD".equals(order.getPaymentMethod()))throw ApiException.conflict("Cash collection requires confirmed COD order");
    // Checkout normally creates PENDING COD payment already. Record collection
    // on that exact payment rather than creating a duplicate financial record.
    Payment payment=payments.forOrder(order.getId()).orElseGet(()->payments.record(order,"PENDING"));
    Payment collection=new Payment();collection.setStatus("PAID");
    return payments.update(payment.getId(),collection);
  }
  @PutMapping("/payments/{id}")public Object changePayment(@PathVariable String id,@RequestBody Payment p){return payments.update(id,p);}
  @DeleteMapping("/payments/{id}")public Object removePayment(@PathVariable String id){payments.delete(id);return Map.of("ok",true);}
}
