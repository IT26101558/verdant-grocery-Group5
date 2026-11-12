package com.verdant.grocery.deliveries.service;
import com.verdant.grocery.deliveries.model.Delivery;
import com.verdant.grocery.orders.service.OrderService;
import com.verdant.grocery.common.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.*;
/**
 * Owner: Kishangi — delivery management and customer tracking (CRUD).
 * New deliveries require an already CONFIRMED order, so a pending/failed demo
 * payment cannot start shipping. Tracking is mirrored to a separate TXT file.
 */
// Owner: Kishangi. Delivery records and tracking are independent of payment state.
@Service public class DeliveryService extends CrudService<Delivery> {
  private final OrderService orders;private final JsonLineStore<Delivery> trackingStore;
  public DeliveryService(@Value("${app.data-dir:data}")String dir,OrderService orders){super(new JsonLineStore<>(dir,"deliveries/records/deliveries.txt",Delivery.class));this.orders=orders;this.trackingStore=new JsonLineStore<>(dir,"deliveries/tracking/tracking.txt",Delivery.class);}
  protected String prefix(){return "DEL";}
  // Protects against creating deliveries while an online payment is pending/failed.
  protected void validate(Delivery d){var order=orders.get(Checks.required(d.getOrderId(),"Order"));if(!"CONFIRMED".equals(order.getStatus()))throw ApiException.conflict("Delivery requires confirmed order");if(!order.getCustomerId().equals(d.getCustomerId()))throw ApiException.bad("Delivery customer mismatch");d.setAddress(order.getAddress());if(d.getStatus()==null)d.setStatus("PROCESSING");if(!List.of("PROCESSING","PACKED","IN_TRANSIT","DELIVERED").contains(d.getStatus()))throw ApiException.bad("Invalid delivery status");if(d.getDriver()==null)d.setDriver("Not assigned");}
  @Override public synchronized Delivery create(Delivery d){if(all().stream().anyMatch(x->x.getOrderId().equals(d.getOrderId())))throw ApiException.conflict("Delivery already exists for order");d.setTrackingCode(Checks.id("TRACK"));Delivery created=super.create(d);trackingStore.save(created);return created;}
  @Override public synchronized Delivery update(String id,Delivery d){Delivery old=get(id);d.setOrderId(old.getOrderId());d.setCustomerId(old.getCustomerId());d.setTrackingCode(old.getTrackingCode());Delivery updated=super.update(id,d);trackingStore.save(updated);return updated;}
  @Override public synchronized void delete(String id){if("DELIVERED".equals(get(id).getStatus()))throw ApiException.conflict("Delivered records cannot be deleted");super.delete(id);trackingStore.remove(id);}
  public Optional<Delivery> forOrder(String orderId){return all().stream().filter(d->d.getOrderId().equals(orderId)).findFirst();}
  public Delivery createFor(com.verdant.grocery.orders.model.Order o){Delivery d=new Delivery();d.setOrderId(o.getId());d.setCustomerId(o.getCustomerId());d.setAddress(o.getAddress());d.setStatus("PROCESSING");d.setDriver("Not assigned");d.setEstimatedDate(java.time.LocalDate.now().plusDays(2).toString());return create(d);}
}
