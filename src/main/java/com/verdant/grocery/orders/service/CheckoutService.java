package com.verdant.grocery.orders.service;
import com.verdant.grocery.common.*;
import com.verdant.grocery.orders.model.*;
import com.verdant.grocery.products.model.Product;
import com.verdant.grocery.products.service.ProductService;
import com.verdant.grocery.inventory.service.InventoryService;
import com.verdant.grocery.customers.service.CustomerService;
import com.verdant.grocery.deliveries.service.DeliveryService;
import com.verdant.grocery.payments.service.PaymentService;
import com.verdant.grocery.payments.model.Payment;
import com.verdant.grocery.payments.strategy.*;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import java.time.*;
import java.math.*;
import java.util.*;
/**
 * Owner: Mathushalini (checkout) with Saraniha (payment integration).
 * The server recomputes line prices and totals; it never trusts a browser total.
 * Cash-on-delivery confirms immediately, whereas demo card orders remain pending
 * until the server's simulated authorization commits reserved stock.
 * Cross-file compensation is best-effort, not a substitute for a database transaction.
 */
// Owner: Mathushalini checkout orchestration, Saraniha payment transition.
// All authorization/stock/total decisions are recalculated by the Java server.
@Service public class CheckoutService {
 private static final BigDecimal DELIVERY_FEE=new BigDecimal("350.00");
 private final CartService carts;private final ProductService products;private final InventoryService inventory;
 private final CustomerService customers;private final OrderService orders;private final PaymentService payments;private final DeliveryService deliveries;
 private final Map<String,PaymentProcessor> processors=new HashMap<>();
 public CheckoutService(CartService carts,ProductService products,InventoryService inventory,CustomerService customers,OrderService orders,PaymentService payments,DeliveryService deliveries,List<PaymentProcessor> strategies){
   this.carts=carts;this.products=products;this.inventory=inventory;this.customers=customers;this.orders=orders;this.payments=payments;this.deliveries=deliveries;for(var strategy:strategies)processors.put(strategy.method(),strategy);
 }
 // Re-read current products and stock: cart/browser prices cannot be trusted.
 private List<OrderLine> makeLines(List<CartLine> cart){
   if(cart==null||cart.isEmpty())throw ApiException.bad("Your cart is empty");List<OrderLine> lines=new ArrayList<>();
   for(CartLine x:cart){Product p=products.get(x.getProductId());if(!p.getActive())throw ApiException.conflict(p.getName()+" is unavailable");
     if(x.getQuantity()<1||inventory.available(p.getId())<x.getQuantity())throw ApiException.conflict("Insufficient stock for "+p.getName());
     OrderLine l=new OrderLine();l.setProductId(p.getId());l.setName(p.getName());l.setUnit(p.getUnit());l.setUnitPrice(p.getPrice());l.setQuantity(x.getQuantity());l.setLineTotal(p.getPrice().multiply(BigDecimal.valueOf(x.getQuantity())));lines.add(l);
   }return lines;
 }
 private BigDecimal subtotal(List<OrderLine> lines){return lines.stream().map(OrderLine::getLineTotal).reduce(BigDecimal.ZERO,BigDecimal::add).setScale(2,RoundingMode.HALF_UP);}
 public synchronized Map<String,Object> quote(String customerId){List<OrderLine> lines=makeLines(carts.get(customerId).getItems());BigDecimal sub=subtotal(lines);return Map.of("items",lines,"subtotal",sub,"deliveryFee",DELIVERY_FEE,"total",sub.add(DELIVERY_FEE));}
 public synchronized Order place(String customerId,String method,String key){return make(customerId,carts.get(customerId).getItems(),method,key,true);}
 public synchronized Order manualCod(String customerId,String productId,int quantity){CartLine line=new CartLine();line.setProductId(productId);line.setQuantity(quantity);return make(customerId,List.of(line),"COD",Checks.id("MANUAL"),false);}
 private Order make(String customerId,List<CartLine> cart,String method,String key,boolean clearCart){
   if(!processors.containsKey(method))throw ApiException.bad("Choose Cash on Delivery or Demo Card");
   if(key==null||key.isBlank()||key.length()>80)throw ApiException.bad("Invalid checkout request key");
   // Idempotency stops a retry or double-click from creating duplicate orders.
   for(Order old:orders.all())if(old.getCustomerId().equals(customerId)&&key.equals(old.getIdempotencyKey())){
     // Reusing a key after a failed/cancelled checkout must not return a dead order.
     if("CANCELLED".equals(old.getStatus()))throw ApiException.conflict("This checkout was cancelled. Start a new checkout.");
     if(!method.equals(old.getPaymentMethod()))throw ApiException.conflict("Checkout request key was used for a different payment method");
     return old;
   }
   List<OrderLine> lines=makeLines(cart);var customer=customers.get(customerId);
   if(!customer.getActive()||!"CUSTOMER".equals(customer.getRole()))throw ApiException.denied();
   Order o=new Order(customerId);o.setItems(lines);o.setSubtotal(subtotal(lines));o.setDeliveryFee(DELIVERY_FEE);o.setTotal(o.getSubtotal().add(DELIVERY_FEE));
   o.setAddress(customer.getAddress());o.setCreatedAt(Instant.now());o.setExpiresAt(Instant.now().plus(Duration.ofMinutes(15)));o.setPaymentMethod(method);o.setIdempotencyKey(key);
   o.setStatus(method.equals("COD")?"PROCESSING":"PENDING_PAYMENT");
   List<OrderLine> changed=new ArrayList<>();boolean created=false;
   try{
     // Reserve for demo card; COD can commit stock when the order is confirmed.
     if(method.equals("DEMO_CARD"))for(OrderLine x:lines){inventory.reserve(x.getProductId(),x.getQuantity());changed.add(x);}
     else for(OrderLine x:lines){inventory.sell(x.getProductId(),x.getQuantity());changed.add(x);}
     orders.create(o);created=true;
     payments.record(o,"PENDING");
     if(method.equals("COD")){
       orders.transition(o,"CONFIRMED");deliveries.createFor(o);
       if(clearCart)carts.removePurchased(customerId,lines);
     }
     return o;
   }catch(RuntimeException e){
     for(OrderLine x:changed)try{if(method.equals("DEMO_CARD"))inventory.release(x.getProductId(),x.getQuantity());else inventory.restoreQuantity(x.getProductId(),x.getQuantity());}catch(Exception ignored){}
     if(created){
       // A delivery written before a later failure must never stay active
       // for an order whose stock and payment are being rolled back.
       try{deliveries.forOrder(o.getId()).ifPresent(d->deliveries.delete(d.getId()));}catch(Exception ignored){}
       // Preserve a failed state for investigation without leaving a misleading
       // pending payment or order behind after a partial cross-file failure.
       try{payments.forOrder(o.getId()).ifPresent(payments::setFailed);}catch(Exception ignored){}
       try{orders.transition(o,"CANCELLED");}catch(Exception ignored){}
     }
     throw e;
   }
 }
 public synchronized Order approveDemo(String customerId,String id){
   Order o=owned(customerId,id);if(!"DEMO_CARD".equals(o.getPaymentMethod()))throw ApiException.bad("Not a demo-card order");
   // A second approval request must never deduct stock or create another delivery.
   if("CONFIRMED".equals(o.getStatus()))return o; // safe retry / duplicate click
   if(!"PENDING_PAYMENT".equals(o.getStatus()))throw ApiException.conflict("Order is not awaiting payment");
   if(o.getExpiresAt()!=null&&Instant.now().isAfter(o.getExpiresAt())){cancelPending(o);throw ApiException.conflict("Payment window expired; try checkout again");}
   Payment payment=payments.forOrder(o.getId()).orElseThrow(()->ApiException.conflict("Payment record missing"));
   // Server chooses the simulated result; no full card number, CVC, or real gateway data.
   if(!processors.get("DEMO_CARD").authorize("SYNTHETIC-TEST-APPROVE"))throw ApiException.conflict("Demo authorization declined");
   List<OrderLine> committed=new ArrayList<>();
   try{
     for(OrderLine x:o.getItems()){inventory.commitReserved(x.getProductId(),x.getQuantity());committed.add(x);}
     payments.setPaid(payment);orders.transition(o,"CONFIRMED");deliveries.createFor(o);
     carts.removePurchased(customerId,o.getItems());return o;
   }catch(RuntimeException e){
     try{deliveries.forOrder(o.getId()).ifPresent(d->deliveries.delete(d.getId()));}catch(Exception ignored){}
     // Best-effort rollback if a cross-file write fails (not equivalent to a database transaction).
     // A failed charge cannot stay marked CONFIRMED or leave a paid receipt.
     for(OrderLine x:o.getItems())try{
       if(committed.contains(x))inventory.restoreQuantity(x.getProductId(),x.getQuantity());
       else inventory.release(x.getProductId(),x.getQuantity());
     }catch(Exception rollback){e.addSuppressed(rollback);}
     try{payments.setFailed(payment);}catch(Exception rollback){e.addSuppressed(rollback);}
     try{orders.transition(o,"CANCELLED");}catch(Exception rollback){e.addSuppressed(rollback);}
     throw e;
   }
 }
 public synchronized Order declineDemo(String customerId,String id){Order o=owned(customerId,id);cancelPending(o);return o;}
 public synchronized void cancelAdmin(String id){Order o=orders.get(id);if(!List.of("PENDING_PAYMENT","CANCELLED").contains(o.getStatus()))throw ApiException.conflict("Only unpaid pending or cancelled orders may be removed");cancelPending(o);payments.forOrder(id).ifPresent(p->{if(!"PAID".equals(p.getStatus()))payments.delete(p.getId());});orders.delete(id);}
 /** Release temporary reservations that have outlived their payment window. */
 @Scheduled(fixedDelayString="${app.pending-cleanup-ms:60000}")
 public synchronized void cleanupExpiredPayments(){
   for(Order o:orders.all()){
     if("PENDING_PAYMENT".equals(o.getStatus()) && o.getExpiresAt()!=null && Instant.now().isAfter(o.getExpiresAt())){
       try{cancelPending(o);}catch(RuntimeException ex){System.err.println("Could not expire order "+o.getId()+": "+ex.getMessage());}
     }
   }
 }
 private Order owned(String customerId,String id){Order o=orders.get(id);if(!o.getCustomerId().equals(customerId))throw ApiException.denied();return o;}
 private void cancelPending(Order o){if("CANCELLED".equals(o.getStatus()))return;if(!"PENDING_PAYMENT".equals(o.getStatus()))throw ApiException.conflict("Cannot cancel confirmed order");
   for(OrderLine l:o.getItems())inventory.release(l.getProductId(),l.getQuantity());orders.transition(o,"CANCELLED");payments.forOrder(o.getId()).ifPresent(p->payments.setFailed(p));
 }
}
