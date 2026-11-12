package com.verdant.grocery.payments.model;
import com.verdant.grocery.common.BaseRecord;
public class Payment extends BaseRecord {
  private String orderId;
  private String customerId;
  private String method;
  private String status;
  private java.math.BigDecimal amount;
  private String transactionReference;
  private String maskedReference;
  private java.time.Instant createdAt;
  private boolean archived;
  public Payment(){}
  public Payment(String orderId,String method){this.orderId=orderId;this.method=method;}
  public String getOrderId(){return orderId;}
  public void setOrderId(String value){this.orderId=value;}
  public String getCustomerId(){return customerId;}
  public void setCustomerId(String value){this.customerId=value;}
  public String getMethod(){return method;}
  public void setMethod(String value){this.method=value;}
  public String getStatus(){return status;}
  public void setStatus(String value){this.status=value;}
  public java.math.BigDecimal getAmount(){return amount;}
  public void setAmount(java.math.BigDecimal value){this.amount=value;}
  public String getTransactionReference(){return transactionReference;}
  public void setTransactionReference(String value){this.transactionReference=value;}
  public String getMaskedReference(){return maskedReference;}
  public void setMaskedReference(String value){this.maskedReference=value;}
  public java.time.Instant getCreatedAt(){return createdAt;}
  public void setCreatedAt(java.time.Instant value){this.createdAt=value;}
  public boolean getArchived(){return archived;}
  public void setArchived(boolean value){this.archived=value;}
}
