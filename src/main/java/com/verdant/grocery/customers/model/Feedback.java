package com.verdant.grocery.customers.model;
import com.verdant.grocery.common.BaseRecord;
public class Feedback extends BaseRecord {
  private String customerId;
  private int rating;
  private String comment;
  private String reply;
  public Feedback(){}
  public String getCustomerId(){return customerId;}
  public void setCustomerId(String value){this.customerId=value;}
  public int getRating(){return rating;}
  public void setRating(int value){this.rating=value;}
  public String getComment(){return comment;}
  public void setComment(String value){this.comment=value;}
  public String getReply(){return reply;}
  public void setReply(String value){this.reply=value;}
}
