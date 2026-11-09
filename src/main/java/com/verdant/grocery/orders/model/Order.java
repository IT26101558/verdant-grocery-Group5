package com.verdant.grocery.orders.model;
import com.verdant.grocery.common.BaseRecord;
public class Order extends BaseRecord {
  private String customerId;
  private java.util.List<OrderLine> items;
  private java.math.BigDecimal subtotal;
  private java.math.BigDecimal deliveryFee;
  private java.math.BigDecimal total;
  private String address;
  private String status;
  private String paymentMethod;
  private java.time.Instant createdAt;
  private java.time.Instant expiresAt;
  private String idempotencyKey;
  public Order(){super();}
  public Order(String customerId){this();this.customerId=customerId;this.items=new java.util.ArrayList<>();}
  public String getCustomerId(){return customerId;}
  public void setCustomerId(String value){this.customerId=value;}
  public java.util.List<OrderLine> getItems(){return items;}
  public void setItems(java.util.List<OrderLine> value){this.items=value;}
  public java.math.BigDecimal getSubtotal(){return subtotal;}
  public void setSubtotal(java.math.BigDecimal value){this.subtotal=value;}
  public java.math.BigDecimal getDeliveryFee(){return deliveryFee;}
  public void setDeliveryFee(java.math.BigDecimal value){this.deliveryFee=value;}
  public java.math.BigDecimal getTotal(){return total;}
  public void setTotal(java.math.BigDecimal value){this.total=value;}
  public String getAddress(){return address;}
  public void setAddress(String value){this.address=value;}
  public String getStatus(){return status;}
  public void setStatus(String value){this.status=value;}
  public String getPaymentMethod(){return paymentMethod;}
  public void setPaymentMethod(String value){this.paymentMethod=value;}
  public java.time.Instant getCreatedAt(){return createdAt;}
  public void setCreatedAt(java.time.Instant value){this.createdAt=value;}
  public java.time.Instant getExpiresAt(){return expiresAt;}
  public void setExpiresAt(java.time.Instant value){this.expiresAt=value;}
  public String getIdempotencyKey(){return idempotencyKey;}
  public void setIdempotencyKey(String value){this.idempotencyKey=value;}
}
