package com.verdant.grocery.orders.service;
import com.verdant.grocery.orders.model.Order;
import com.verdant.grocery.common.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.*;
/**
 * Owner: Mathushalini — order CRUD and status transitions.
 * A canonical order remains in the pending/orders.txt store even after its status
 * changes; separate confirmed/cancelled files are status snapshots for review.
 */
// Owner: Mathushalini. Order records persist; confirmation/cancellation are controlled transitions.
@Service public class OrderService extends CrudService<Order> {
  private final JsonLineStore<Order> confirmedStore;private final JsonLineStore<Order> cancelledStore;
  public OrderService(@Value("${app.data-dir:data}")String dir){super(new JsonLineStore<>(dir,"orders/pending/orders.txt",Order.class));confirmedStore=new JsonLineStore<>(dir,"orders/confirmed/confirmed.txt",Order.class);cancelledStore=new JsonLineStore<>(dir,"orders/cancelled/cancelled.txt",Order.class);}
  protected String prefix(){return "ORD";}
  protected void validate(Order o){Checks.required(o.getCustomerId(),"Customer");Checks.required(o.getAddress(),"Address");if(o.getItems()==null||o.getItems().isEmpty())throw ApiException.bad("An order requires items");if(o.getTotal()==null||o.getTotal().signum()<0)throw ApiException.bad("Invalid total");}
  @Override public synchronized Order update(String id,Order supplied){
    Order existing=get(id);if(!List.of("PENDING_PAYMENT","CANCELLED").contains(existing.getStatus()))throw ApiException.conflict("Confirmed orders cannot be edited");
    // Admin can update the address; price, items, and payment state are immutable.
    existing.setAddress(Checks.required(supplied.getAddress(),"Address"));store.save(existing);return existing;
  }
  // Keep canonical status and review snapshots in their respective TXT files.
  public synchronized void transition(Order o,String status){
    o.setStatus(status);store.save(o);
    // Keep status snapshots consistent when a multi-file checkout is rolled back.
    if("CONFIRMED".equals(status))confirmedStore.save(o);
    else if(confirmedStore.find(o.getId()).isPresent())confirmedStore.remove(o.getId());
    if("CANCELLED".equals(status))cancelledStore.save(o);
    else if(cancelledStore.find(o.getId()).isPresent())cancelledStore.remove(o.getId());
  }
  @Override public synchronized void delete(String id){
    Order o=get(id);if(!"CANCELLED".equals(o.getStatus()))throw ApiException.conflict("Cancel this order before deleting it");
    super.delete(id);
    if(confirmedStore.find(id).isPresent())confirmedStore.remove(id);
    if(cancelledStore.find(id).isPresent())cancelledStore.remove(id);
  }
}
