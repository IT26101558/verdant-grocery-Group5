package com.verdant.grocery.customers.model;
import com.verdant.grocery.common.BaseRecord;
// Owner: Vakeesan. Address copy for the dedicated customer/address file area.
public class AddressRecord extends BaseRecord {
 private String customerId; private String address;
 public AddressRecord(){}
 public AddressRecord(String customerId,String address){this.customerId=customerId;this.address=address;setId(customerId);}
 public String getCustomerId(){return customerId;}
 public void setCustomerId(String v){customerId=v;}
 public String getAddress(){return address;}
 public void setAddress(String v){address=v;}
}
