package com.verdant.grocery.inventory.model;
import com.verdant.grocery.common.BaseRecord;
public class Stock extends BaseRecord {
  private String productId;
  private int quantity;
  private int reserved;
  private int reorderLevel;
  public Stock(){}
  public String getProductId(){return productId;}
  public void setProductId(String value){this.productId=value;}
  public int getQuantity(){return quantity;}
  public void setQuantity(int value){this.quantity=value;}
  public int getReserved(){return reserved;}
  public void setReserved(int value){this.reserved=value;}
  public int getReorderLevel(){return reorderLevel;}
  public void setReorderLevel(int value){this.reorderLevel=value;}
}
