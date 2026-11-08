package com.verdant.grocery.common;
import com.verdant.grocery.customers.service.CustomerService;
import com.verdant.grocery.products.service.ProductService;
import com.verdant.grocery.inventory.service.InventoryService;
import com.verdant.grocery.products.model.Product;
import com.verdant.grocery.inventory.model.Stock;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import java.math.*;
import java.nio.file.*;
/**
 * Development bootstrap: creates required data directories and demo-only sample records.
 * Existing records are kept on restart rather than recreated, preserving file persistence.
 */
@Configuration public class DemoSeed {
 @Bean ApplicationRunner seed(CustomerService customers,ProductService products,InventoryService inventory,
     @Value("${app.demo-admin-email}")String adminEmail,@Value("${app.demo-admin-password}")String adminPassword,
     @Value("${app.data-dir:data}")String dir){return args->{
  for(String folder:new String[]{"customers/accounts","customers/addresses","customers/feedback","products/catalogue","products/categories","inventory/stock","inventory/adjustments","inventory/alerts","orders/carts","orders/pending","orders/confirmed","orders/cancelled","deliveries/records","deliveries/tracking","payments/records","payments/transactions","payments/receipts"})Files.createDirectories(Path.of(dir,folder));
  customers.seedAdmin(adminEmail,adminPassword);
  if(products.all().isEmpty()){
   String[][] sample={
     {"Red Apples","fruits","Fresh crunchy apples","1 kg","850","🍎"},{"Bananas","fruits","Sweet local banana bunch","1 kg","360","🍌"},
     {"Carrots","vegetables","Farm-fresh orange carrots","500 g","240","🥕"},{"Tomatoes","vegetables","Ripe tomatoes from local farms","1 kg","420","🍅"},
     {"Fresh Milk","dairy-eggs","Creamy pasteurized fresh milk","1 L","510","🥛"},{"Farm Eggs","dairy-eggs","Fresh medium eggs","6 pack","480","🥚"},
     {"Wholegrain Bread","bakery","Soft baked wheat loaf","1 loaf","390","🍞"},{"Croissants","bakery","Golden butter pastries","2 pack","590","🥐"},
     {"Fresh Fish","meat-seafood","Cleaned fresh fish portions","500 g","1240","🐟"},{"Chicken Breast","meat-seafood","Boneless chicken breast","500 g","1190","🍗"},
     {"Red Rice","grains-pulses","Sri Lankan wholegrain red rice","1 kg","360","🍚"},{"Green Gram","grains-pulses","Fresh green mung beans","500 g","440","🫘"},
     {"Orange Juice","beverages","Refreshing orange drink","1 L","690","🧃"},{"Coconut Water","beverages","Natural refreshing coconut water","500 ml","320","🥥"},
     {"Cashew Nuts","snacks","Roasted crunchy cashews","200 g","990","🥜"},{"Potato Chips","snacks","Crispy lightly salted chips","150 g","350","🥔"},
     {"Frozen Peas","frozen-foods","Quick-frozen green peas","500 g","660","🫛"},{"Frozen Berries","frozen-foods","Mixed berries in a resealable pack","300 g","1490","🫐"},
     {"Dish Soap","household","Gentle dishwashing liquid","500 ml","450","🧼"},{"Paper Towels","household","Absorbent household rolls","2 rolls","550","🧻"}
   };
   for(String[] v:sample){Product p=new Product();p.setName(v[0]);p.setCategory(v[1]);p.setDescription(v[2]);p.setUnit(v[3]);p.setPrice(new BigDecimal(v[4]));p.setImageUrl(v[5]);p.setActive(true);p=products.create(p);Stock s=new Stock();s.setProductId(p.getId());s.setQuantity(35);s.setReorderLevel(5);inventory.create(s);}
  }
 };}
}
