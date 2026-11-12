package com.verdant.grocery.orders.model;
import com.verdant.grocery.common.BaseRecord;
public class Cart extends BaseRecord {
  private String customerId;
  private java.util.List<CartLine> items;
  public Cart(){}
  public Cart(String customerId){this.customerId=customerId;this.items=new java.util.ArrayList<>();}
  public String getCustomerId(){return customerId;}
  public void setCustomerId(String value){this.customerId=value;}
  public java.util.List<CartLine> getItems(){return items;}
  public void setItems(java.util.List<CartLine> value){this.items=value;}
}
