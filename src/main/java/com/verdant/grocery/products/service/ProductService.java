package com.verdant.grocery.products.service;
import com.verdant.grocery.products.model.Product;
import com.verdant.grocery.common.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.*;
/**
 * Owner: Sujeethan — category-aware product catalogue (CRUD).
 * Soft deletion hides a product but preserves its identity in historical orders.
 */
// Owner: Sujeethan. Product catalogue: add, read, update, soft-delete archived products.
@Service public class ProductService extends CrudService<Product> {
  public static final List<String> CATEGORIES=List.of("fruits","vegetables","dairy-eggs","bakery","meat-seafood","grains-pulses","beverages","snacks","frozen-foods","household");
  public ProductService(@Value("${app.data-dir:data}")String dir){super(new JsonLineStore<>(dir,"products/catalogue/products.txt",Product.class));}
  protected String prefix(){return "PRD";}
  protected void validate(Product p){p.setName(Checks.required(p.getName(),"Product name"));p.setDescription(Checks.required(p.getDescription(),"Description"));p.setUnit(Checks.required(p.getUnit(),"Unit"));p.setPrice(Checks.positive(p.getPrice(),"Price"));if(!CATEGORIES.contains(p.getCategory()))throw ApiException.bad("Invalid category");if(p.getImageUrl()==null)p.setImageUrl("");}
  public List<Product> active(){return all().stream().filter(Product::getActive).toList();}
  // Soft-delete: old order lines may still reference this product ID.
  @Override public synchronized void delete(String id){Product p=get(id);p.setActive(false);store.save(p);}
}
