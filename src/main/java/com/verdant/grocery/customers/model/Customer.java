package com.verdant.grocery.customers.model;
import com.verdant.grocery.common.BaseRecord;
public class Customer extends BaseRecord {
  private String fullName;
  private String email;
  private String mobile;
  private String address;
  private String passwordHash;
  private String role;
  private boolean active;
  public Customer(){super();}
  public Customer(String name,String email){this();this.fullName=name;this.email=email;this.role="CUSTOMER";this.active=true;}
  public String getFullName(){return fullName;}
  public void setFullName(String value){this.fullName=value;}
  public String getEmail(){return email;}
  public void setEmail(String value){this.email=value;}
  public String getMobile(){return mobile;}
  public void setMobile(String value){this.mobile=value;}
  public String getAddress(){return address;}
  public void setAddress(String value){this.address=value;}
  public String getPasswordHash(){return passwordHash;}
  public void setPasswordHash(String value){this.passwordHash=value;}
  public String getRole(){return role;}
  public void setRole(String value){this.role=value;}
  public boolean getActive(){return active;}
  public void setActive(boolean value){this.active=value;}
}
