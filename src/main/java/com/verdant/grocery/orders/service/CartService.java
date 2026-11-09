package com.verdant.grocery.orders.service;
import com.verdant.grocery.orders.model.*;
import com.verdant.grocery.products.service.ProductService;
import com.verdant.grocery.inventory.service.InventoryService;
import com.verdant.grocery.common.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.*;
/**
 * Owner: Mathushalini — persistent cart items (add/read/update/remove).
 * Cart quantities are validated now and rechecked at checkout, because stock
 * and prices can change while a user is shopping.
 */
// Owner: Mathushalini. A customer's cart is persisted as JSON lines in the orders folder.
@Service public class CartService {
  private final JsonLineStore<Cart> store;private final ProductService products;private final InventoryService stock;
  public CartService(@Value("${app.data-dir:data}")String dir,ProductService products,InventoryService stock){store=new JsonLineStore<>(dir,"orders/carts/carts.txt",Cart.class);this.products=products;this.stock=stock;}
  public synchronized Cart get(String customerId){Cart c=store.all().stream().filter(item->customerId.equals(item.getCustomerId())).findFirst().orElseGet(()->{Cart fresh=new Cart(customerId);fresh.setId(Checks.id("CART"));store.save(fresh);return fresh;});if(c.getItems()==null)c.setItems(new ArrayList<>());return c;}
  public synchronized Cart put(String customerId,String productId,int quantity){
    if(quantity<0||quantity>99)throw ApiException.bad("Quantity must be between 0 and 99");
    if(quantity>0&&!products.get(productId).getActive())throw ApiException.bad("Product unavailable");
    if(quantity>0 && quantity>stock.available(productId))throw ApiException.conflict("Insufficient stock");
    Cart cart=get(customerId);if(cart.getItems()==null)cart.setItems(new ArrayList<>());
    cart.getItems().removeIf(i->i.getProductId().equals(productId));
    if(quantity>0){CartLine l=new CartLine();l.setProductId(productId);l.setQuantity(quantity);cart.getItems().add(l);}store.save(cart);return cart;
  }
  public synchronized void clear(String customerId){Cart c=get(customerId);c.setItems(new ArrayList<>());store.save(c);}
  /**
   * Remove only purchased quantities, not unrelated items added while a
   * simulated payment was awaiting approval. A changed/smaller line is left
   * alone so a customer edit is not silently lost.
   */
  public synchronized void removePurchased(String customerId,List<OrderLine> purchased){
    Cart cart=get(customerId);
    if(cart.getItems()==null)return;
    for(OrderLine bought:purchased){
      for(Iterator<CartLine> it=cart.getItems().iterator();it.hasNext();){
        CartLine line=it.next();
        if(!Objects.equals(line.getProductId(),bought.getProductId()))continue;
        if(line.getQuantity()==bought.getQuantity())it.remove();
        else if(line.getQuantity()>bought.getQuantity())line.setQuantity(line.getQuantity()-bought.getQuantity());
        break;
      }
    }
    store.save(cart);
  }
}
