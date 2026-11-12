package com.verdant.grocery.payments.service;
import com.verdant.grocery.payments.model.Payment;
import com.verdant.grocery.orders.service.OrderService;
import com.verdant.grocery.common.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.*;
import java.time.Instant;
/**
 * Owner: Saraniha — COD and simulated card payments (CRUD).
 * Payment amounts are checked against the authoritative order total.
 * Paid records cannot be edited/deleted; unpaid deletion is an audit-friendly archive.
 * No real PAN, security code, or payment gateway details are persisted.
 */
// Owner: Saraniha. Payment records, receipts, simulated authorization, safe removal policy.
@Service public class PaymentService extends CrudService<Payment> {
  private final OrderService orders;private final JsonLineStore<Payment> transactionStore;private final JsonLineStore<Payment> receiptStore;
  public PaymentService(@Value("${app.data-dir:data}")String dir,OrderService orders){super(new JsonLineStore<>(dir,"payments/records/payments.txt",Payment.class));this.orders=orders;this.transactionStore=new JsonLineStore<>(dir,"payments/transactions/transactions.txt",Payment.class);this.receiptStore=new JsonLineStore<>(dir,"payments/receipts/receipts.txt",Payment.class);}
  protected String prefix(){return "PAY";}
  // Always match the order total calculated by the Java checkout service.
  protected void validate(Payment p){var o=orders.get(Checks.required(p.getOrderId(),"Order"));if(!o.getCustomerId().equals(p.getCustomerId()))throw ApiException.bad("Customer mismatch");if(p.getAmount()==null||p.getAmount().compareTo(o.getTotal())!=0)throw ApiException.bad("Amount must equal order total");if(!List.of("COD","DEMO_CARD").contains(p.getMethod()))throw ApiException.bad("Unsupported payment method");if(!List.of("PENDING","PAID","FAILED").contains(p.getStatus()))throw ApiException.bad("Invalid payment status");}
  @Override public synchronized Payment create(Payment p){if(all().stream().anyMatch(x->x.getOrderId().equals(p.getOrderId())&&!x.getArchived()))throw ApiException.conflict("Payment already exists for order");if(p.getCreatedAt()==null)p.setCreatedAt(Instant.now());Payment created=super.create(p);transactionStore.save(created);return created;}
  @Override public synchronized Payment update(String id,Payment request){
    Payment original=get(id);if(original.getArchived())throw ApiException.conflict("Archived payment");
    if("PAID".equals(original.getStatus()))throw ApiException.conflict("Paid payment cannot be edited");
    if(!"COD".equals(original.getMethod())||!"PAID".equals(request.getStatus()))throw ApiException.bad("Only COD collection can be recorded here");
    if(!"CONFIRMED".equals(orders.get(original.getOrderId()).getStatus()))throw ApiException.conflict("Order not confirmed");
    original.setStatus("PAID");original.setTransactionReference(Checks.id("CASH"));store.save(original);transactionStore.save(original);receiptStore.save(original);return original;
  }
  // Retain paid transactions for history; archive only unpaid records.
  @Override public synchronized void delete(String id){Payment p=get(id);if("PAID".equals(p.getStatus()))throw ApiException.conflict("Paid records must be retained for audit");p.setArchived(true);store.save(p);}
  public Optional<Payment> forOrder(String orderId){return all().stream().filter(p->p.getOrderId().equals(orderId)&&!p.getArchived()).findFirst();}
  public Payment record(com.verdant.grocery.orders.model.Order o,String status){Payment p=new Payment(o.getId(),o.getPaymentMethod());p.setCustomerId(o.getCustomerId());p.setAmount(o.getTotal());p.setStatus(status);p.setCreatedAt(Instant.now());p.setTransactionReference(status.equals("PAID")?Checks.id("DEMO-TXN"):"NOT-COLLECTED");p.setMaskedReference(status.equals("PAID")?"TEST-ONLY •••• 4242":"");return create(p);}
  // Synthetic transaction reference only; never persist real card credentials.
  public synchronized void setPaid(Payment p){p.setStatus("PAID");p.setTransactionReference(Checks.id("DEMO-TXN"));p.setMaskedReference("TEST-ONLY •••• 4242");store.save(p);transactionStore.save(p);receiptStore.save(p);}
  public synchronized void setFailed(Payment p){p.setStatus("FAILED");store.save(p);transactionStore.save(p);
    // A payment that was rolled back must not leave a stale paid receipt.
    if(receiptStore.find(p.getId()).isPresent())receiptStore.remove(p.getId());}
}
