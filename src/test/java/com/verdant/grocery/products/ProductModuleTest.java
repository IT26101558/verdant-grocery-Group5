package com.verdant.grocery.products;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.math.BigDecimal;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest class ProductModuleTest {
 static final Path folder;
 static{try{folder=Files.createTempDirectory("verdant-products-");}catch(Exception e){throw new RuntimeException(e);}}
 @DynamicPropertySource static void properties(DynamicPropertyRegistry r){r.add("app.data-dir",folder::toString);}

 @Autowired com.verdant.grocery.products.service.ProductService products;
 @Test void catalogueCrudAndCategories(){
   var p=new com.verdant.grocery.products.model.Product();p.setName("Fresh test mango");p.setCategory("fruits");p.setDescription("Yellow mango");p.setUnit("1 kg");p.setPrice(new BigDecimal("260.00"));p.setActive(true);
   p=products.create(p);assertEquals(p.getId(),products.get(p.getId()).getId());
   p.setPrice(new BigDecimal("300.00"));products.update(p.getId(),p);assertEquals(0,new BigDecimal("300.00").compareTo(products.get(p.getId()).getPrice()));
   products.delete(p.getId());assertFalse(products.get(p.getId()).getActive());assertEquals(10,com.verdant.grocery.products.service.ProductService.CATEGORIES.size());
 }

}
