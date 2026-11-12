package com.verdant.grocery.inventory.model;
import com.verdant.grocery.common.BaseRecord;
import java.time.Instant;
// Owner: Saarkesh. Append-only adjustment entries for stock movements and reservations.
public class StockChange extends BaseRecord {
 private String productId;private int delta;private String reason;private Instant occurredAt;
 public StockChange(){}
 public StockChange(String productId,int delta,String reason){this.productId=productId;this.delta=delta;this.reason=reason;this.occurredAt=Instant.now();}
 public String getProductId(){return productId;}
 public void setProductId(String v){productId=v;}
 public int getDelta(){return delta;}
 public void setDelta(int v){delta=v;}
 public String getReason(){return reason;}
 public void setReason(String v){reason=v;}
 public Instant getOccurredAt(){return occurredAt;}
 public void setOccurredAt(Instant v){occurredAt=v;}
}
