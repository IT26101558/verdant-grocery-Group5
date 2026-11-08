package com.verdant.grocery.common;
// Encapsulation and constructor chaining shared by all six domains.
/** Shared record superclass; each concrete entity inherits a stable string ID. */
public abstract class BaseRecord {
  private String id;
  protected BaseRecord() {this(null);}
  protected BaseRecord(String id){this.id=id;}
  public String getId(){return id;}
  public void setId(String id){this.id=id;}
}
