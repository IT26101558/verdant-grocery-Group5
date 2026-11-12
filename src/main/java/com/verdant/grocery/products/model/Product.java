package com.verdant.grocery.products.model;
import com.verdant.grocery.common.BaseRecord;
public class Product extends BaseRecord {
  private String name;
  private String category;
  private String description;
  private String unit;
  private java.math.BigDecimal price;
  private String imageUrl;
  private boolean active;
  public Product(){}
  public String getName(){return name;}
  public void setName(String value){this.name=value;}
  public String getCategory(){return category;}
  public void setCategory(String value){this.category=value;}
  public String getDescription(){return description;}
  public void setDescription(String value){this.description=value;}
  public String getUnit(){return unit;}
  public void setUnit(String value){this.unit=value;}
  public java.math.BigDecimal getPrice(){return price;}
  public void setPrice(java.math.BigDecimal value){this.price=value;}
  public String getImageUrl(){return imageUrl;}
  public void setImageUrl(String value){this.imageUrl=value;}
  public boolean getActive(){return active;}
  public void setActive(boolean value){this.active=value;}
}
