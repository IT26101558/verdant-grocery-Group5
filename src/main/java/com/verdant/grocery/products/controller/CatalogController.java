package com.verdant.grocery.products.controller;
import com.verdant.grocery.products.service.ProductService;
import com.verdant.grocery.inventory.service.InventoryService;
import com.verdant.grocery.products.model.Product;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/catalog")public class CatalogController {
 private final ProductService products;private final InventoryService stock;
 public CatalogController(ProductService products,InventoryService stock){this.products=products;this.stock=stock;}
 @GetMapping("/categories")public Object categories(){return ProductService.CATEGORIES;}
 @GetMapping("/products")public Object list(@RequestParam(required=false)String category,@RequestParam(defaultValue="")String q){
   return products.active().stream().filter(p->category==null||category.equals(p.getCategory()))
     .filter(p->p.getName().toLowerCase().contains(q.toLowerCase())||p.getDescription().toLowerCase().contains(q.toLowerCase())).map(this::view).toList();}
 @GetMapping("/products/{id}")public Object detail(@PathVariable String id){Product p=products.get(id);if(!p.getActive())throw com.verdant.grocery.common.ApiException.notFound("Product unavailable");return view(p);}
 private Map<String,Object> view(Product p){Map<String,Object> m=new LinkedHashMap<>();m.put("id",p.getId());m.put("name",p.getName());m.put("category",p.getCategory());m.put("description",p.getDescription());m.put("unit",p.getUnit());m.put("price",p.getPrice());m.put("imageUrl",p.getImageUrl());m.put("active",p.getActive());try{m.put("available",stock.available(p.getId()));}catch(Exception e){m.put("available",0);}return m;}
}
