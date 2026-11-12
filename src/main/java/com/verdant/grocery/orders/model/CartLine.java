package com.verdant.grocery.orders.model;
public class CartLine {
  private String productId;
  private int quantity;
  public CartLine(){}
  public String getProductId(){return productId;}
  public void setProductId(String value){this.productId=value;}
  public int getQuantity(){return quantity;}
  public void setQuantity(int value){this.quantity=value;}
}
