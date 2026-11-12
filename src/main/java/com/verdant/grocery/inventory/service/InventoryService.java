package com.verdant.grocery.inventory.service;
import com.verdant.grocery.inventory.model.Stock;
import com.verdant.grocery.inventory.model.StockChange;
import com.verdant.grocery.products.service.ProductService;
import com.verdant.grocery.common.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.*;
/**
 * Owner: Saarkesh — inventory with on-hand and reserved quantities.
 * Available stock = on-hand minus reserved stock; this prevents overselling
 * during a pending demo payment. Adjustments and low-stock alerts are persisted.
 */
// Owner: Saarkesh. One stock row per product; reserved vs on-hand ensures no overselling.
@Service public class InventoryService extends CrudService<Stock>{
  private final ProductService products;private final JsonLineStore<StockChange> adjustments;private final JsonLineStore<Stock> alerts;
  public InventoryService(@Value("${app.data-dir:data}")String dir,ProductService products){super(new JsonLineStore<>(dir,"inventory/stock/stock.txt",Stock.class));this.products=products;adjustments=new JsonLineStore<>(dir,"inventory/adjustments/adjustments.txt",StockChange.class);alerts=new JsonLineStore<>(dir,"inventory/alerts/alerts.txt",Stock.class);}
  protected String prefix(){return "STK";}
  protected void validate(Stock s){products.get(Checks.required(s.getProductId(),"Product"));Checks.nonnegative(s.getQuantity(),"Quantity");Checks.nonnegative(s.getReserved(),"Reserved");Checks.nonnegative(s.getReorderLevel(),"Reorder level");if(s.getReserved()>s.getQuantity())throw ApiException.bad("Reserved exceeds quantity");}
  @Override public synchronized Stock create(Stock s){
    if(all().stream().anyMatch(x->x.getProductId().equals(s.getProductId())))throw ApiException.conflict("Product already has a stock record");
    // Admin sets physical on-hand quantity; only checkout may reserve units.
    // Never accept a browser-provided reserved count as an actual reservation.
    s.setReserved(0);
    Stock created=super.create(s);log(created,created.getQuantity(),"INITIAL_STOCK");return created;
  }
  @Override public synchronized Stock update(String id,Stock input){Stock old=get(id);input.setProductId(old.getProductId());input.setReserved(old.getReserved());Stock updated=super.update(id,input);log(updated,updated.getQuantity()-old.getQuantity(),"MANUAL_ADJUSTMENT");return updated;}
  @Override public synchronized void delete(String id){Stock s=get(id);if(s.getQuantity()>0||s.getReserved()>0)throw ApiException.conflict("Reduce stock to zero before removing the record");log(s,0,"RECORD_DELETED");super.delete(id);if(alerts.find(id).isPresent())alerts.remove(id);}
  public Stock forProduct(String id){return all().stream().filter(s->s.getProductId().equals(id)).findFirst().orElseThrow(()->ApiException.notFound("Stock record missing for product"));}
  // Pending card checkouts reserve units without reducing physical stock.
  public int available(String id){Stock s=forProduct(id);return s.getQuantity()-s.getReserved();}
  public synchronized void reserve(String id,int count){Stock s=forProduct(id);if(count<1||available(id)<count)throw ApiException.conflict("Insufficient stock");s.setReserved(s.getReserved()+count);store.save(s);log(s,0,"RESERVE_"+count);}
  public synchronized void release(String id,int count){Stock s=forProduct(id);if(count<1||s.getReserved()<count)throw ApiException.conflict("Invalid stock release");s.setReserved(s.getReserved()-count);store.save(s);log(s,0,"RELEASE_"+count);}
  // Only a completed server-side payment converts a reservation into a sale.
  public synchronized void commitReserved(String id,int count){Stock s=forProduct(id);if(s.getReserved()<count||s.getQuantity()<count)throw ApiException.conflict("Stock reservation not available");s.setReserved(s.getReserved()-count);s.setQuantity(s.getQuantity()-count);store.save(s);log(s,-count,"STOCK_COMMITTED");}
  public synchronized void sell(String id,int count){Stock s=forProduct(id);if(count<1||available(id)<count)throw ApiException.conflict("Insufficient stock");s.setQuantity(s.getQuantity()-count);store.save(s);log(s,-count,"CASH_ORDER");}
  public synchronized void restoreQuantity(String id,int count){Stock s=forProduct(id);s.setQuantity(s.getQuantity()+count);store.save(s);log(s,count,"STOCK_RESTORED");}
  private void log(Stock s,int delta,String reason){
    StockChange change=new StockChange(s.getProductId(),delta,reason);change.setId(Checks.id("ADJ"));adjustments.save(change);
    if(s.getQuantity()-s.getReserved()<=s.getReorderLevel())alerts.save(s);
    else if(alerts.find(s.getId()).isPresent())alerts.remove(s.getId());
  }
  public List<Stock> low(){return all().stream().filter(s->s.getQuantity()-s.getReserved()<=s.getReorderLevel()).toList();}
}
