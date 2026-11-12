package com.verdant.grocery.inventory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.math.BigDecimal;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest class InventoryModuleTest {
 static final Path folder;
 static{try{folder=Files.createTempDirectory("verdant-inventory-");}catch(Exception e){throw new RuntimeException(e);}}
 @DynamicPropertySource static void properties(DynamicPropertyRegistry r){r.add("app.data-dir",folder::toString);}

 @Autowired com.verdant.grocery.products.service.ProductService products;
 @Autowired com.verdant.grocery.inventory.service.InventoryService inventory;
 @Test void stockCrudAndReservations(){
   var p=new com.verdant.grocery.products.model.Product();p.setName("Test pear");p.setCategory("fruits");p.setDescription("Sweet pear");p.setUnit("1 kg");p.setPrice(new BigDecimal("250.00"));p.setActive(true);p=products.create(p);
   var s=new com.verdant.grocery.inventory.model.Stock();s.setProductId(p.getId());s.setQuantity(3);s.setReorderLevel(1);s=inventory.create(s);
   inventory.reserve(p.getId(),2);assertEquals(1,inventory.available(p.getId()));inventory.release(p.getId(),2);
   var edit=new com.verdant.grocery.inventory.model.Stock();edit.setQuantity(0);edit.setReorderLevel(1);inventory.update(s.getId(),edit);inventory.delete(s.getId());
   assertThrows(com.verdant.grocery.common.ApiException.class,()->inventory.get("missing"));
 }

}
