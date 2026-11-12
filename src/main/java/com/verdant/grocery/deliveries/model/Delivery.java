package com.verdant.grocery.deliveries.model;
import com.verdant.grocery.common.BaseRecord;
public class Delivery extends BaseRecord {
  private String orderId;
  private String customerId;
  private String driver;
  private String status;
  private String estimatedDate;
  private String trackingCode;
  private String address;
  public Delivery(){}
  public String getOrderId(){return orderId;}
  public void setOrderId(String value){this.orderId=value;}
  public String getCustomerId(){return customerId;}
  public void setCustomerId(String value){this.customerId=value;}
  public String getDriver(){return driver;}
  public void setDriver(String value){this.driver=value;}
  public String getStatus(){return status;}
  public void setStatus(String value){this.status=value;}
  public String getEstimatedDate(){return estimatedDate;}
  public void setEstimatedDate(String value){this.estimatedDate=value;}
  public String getTrackingCode(){return trackingCode;}
  public void setTrackingCode(String value){this.trackingCode=value;}
  public String getAddress(){return address;}
  public void setAddress(String value){this.address=value;}
}
