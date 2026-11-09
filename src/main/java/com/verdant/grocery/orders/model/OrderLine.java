package com.verdant.grocery.orders.model;
public class OrderLine {
  private String productId;
  private String name;
  private String unit;
  private java.math.BigDecimal unitPrice;
  private int quantity;
  private java.math.BigDecimal lineTotal;
  public OrderLine(){}
  public String getProductId(){return productId;}
  public void setProductId(String value){this.productId=value;}
  public String getName(){return name;}
  public void setName(String value){this.name=value;}
  public String getUnit(){return unit;}
  public void setUnit(String value){this.unit=value;}
  public java.math.BigDecimal getUnitPrice(){return unitPrice;}
  public void setUnitPrice(java.math.BigDecimal value){this.unitPrice=value;}
  public int getQuantity(){return quantity;}
  public void setQuantity(int value){this.quantity=value;}
  public java.math.BigDecimal getLineTotal(){return lineTotal;}
  public void setLineTotal(java.math.BigDecimal value){this.lineTotal=value;}
}
