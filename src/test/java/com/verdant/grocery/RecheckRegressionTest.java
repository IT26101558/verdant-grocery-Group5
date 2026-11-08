package com.verdant.grocery;

import com.verdant.grocery.common.ApiException;
import com.verdant.grocery.common.JsonLineStore;
import com.verdant.grocery.customers.service.CustomerService;
import com.verdant.grocery.deliveries.service.DeliveryService;
import com.verdant.grocery.inventory.model.Stock;
import com.verdant.grocery.inventory.service.InventoryService;
import com.verdant.grocery.orders.model.Order;
import com.verdant.grocery.orders.model.OrderLine;
import com.verdant.grocery.orders.service.CartService;
import com.verdant.grocery.orders.service.CheckoutService;
import com.verdant.grocery.orders.service.OrderService;
import com.verdant.grocery.payments.service.PaymentService;
import com.verdant.grocery.payments.strategy.CashOnDeliveryProcessor;
import com.verdant.grocery.payments.strategy.DemoCardProcessor;
import com.verdant.grocery.products.model.Product;
import com.verdant.grocery.products.service.ProductService;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** Regression cases for checkout cancellation, file snapshots and cart preservation. */
class RecheckRegressionTest {
    private record Setup(String dir, CustomerService customers, ProductService products,
                         InventoryService stock, CartService carts, OrderService orders,
                         PaymentService payments, DeliveryService deliveries,
                         CheckoutService checkout, String customerId, Product product) { }

    private Setup setup(boolean failDelivery) throws Exception {
        String dir=Files.createTempDirectory("verdant-recheck-").toString();
        var customers=new CustomerService(dir);
        var products=new ProductService(dir);
        var stock=new InventoryService(dir,products);
        var carts=new CartService(dir,products,stock);
        var orders=new OrderService(dir);
        var payments=new PaymentService(dir,orders);
        DeliveryService deliveries=failDelivery ? new DeliveryService(dir,orders) {
            @Override public com.verdant.grocery.deliveries.model.Delivery createFor(Order order) {
                throw new IllegalStateException("Simulated delivery file failure");
            }
        } : new DeliveryService(dir,orders);
        var checkout=new CheckoutService(carts,products,stock,customers,orders,payments,deliveries,
            List.of(new CashOnDeliveryProcessor(),new DemoCardProcessor()));
        String customerId=customers.register("Test Customer",UUID.randomUUID()+"@example.com", "0771234567",
            "Colombo", "strongpass123", "strongpass123").getId();
        var p=new Product();p.setName("Papaya");p.setDescription("Fresh local papaya");p.setUnit("1 kg");
        p.setCategory("fruits");p.setPrice(new BigDecimal("250.00"));p.setActive(true);
        p=products.create(p);
        var s=new Stock();s.setProductId(p.getId());s.setQuantity(10);s.setReorderLevel(2);stock.create(s);
        return new Setup(dir,customers,products,stock,carts,orders,payments,deliveries,checkout,customerId,p);
    }

    @Test void cancelledKeyMustNotReturnDeadOrder() throws Exception {
        var a=setup(false);
        a.carts.put(a.customerId,a.product.getId(),2);
        var o=a.checkout.place(a.customerId,"DEMO_CARD","reusable-key");
        a.checkout.declineDemo(a.customerId,o.getId());
        assertThrows(ApiException.class, () -> a.checkout.place(a.customerId,"DEMO_CARD","reusable-key"));
        assertEquals(0,a.stock.forProduct(a.product.getId()).getReserved());
        assertEquals("CANCELLED",a.orders.get(o.getId()).getStatus());
    }

    @Test void approvalKeepsNewCartItemsAndLeavesCorrectSnapshots() throws Exception {
        var a=setup(false);
        a.carts.put(a.customerId,a.product.getId(),2);
        var o=a.checkout.place(a.customerId,"DEMO_CARD","pay-once");
        a.carts.put(a.customerId,a.product.getId(),3);
        a.checkout.approveDemo(a.customerId,o.getId());
        assertEquals(1,a.carts.get(a.customerId).getItems().get(0).getQuantity());
        assertEquals(8,a.stock.forProduct(a.product.getId()).getQuantity());
        assertEquals(0,a.stock.forProduct(a.product.getId()).getReserved());
        assertEquals("PAID",a.payments.forOrder(o.getId()).orElseThrow().getStatus());
        assertTrue(new JsonLineStore<>(a.dir,"orders/confirmed/confirmed.txt",Order.class).find(o.getId()).isPresent());
    }

    @Test void deliveryFailureNeverLeavesConfirmedOrderOrPaidReceipt() throws Exception {
        var a=setup(true);
        a.carts.put(a.customerId,a.product.getId(),2);
        var o=a.checkout.place(a.customerId,"DEMO_CARD","error-case");
        assertThrows(IllegalStateException.class, () -> a.checkout.approveDemo(a.customerId,o.getId()));
        assertEquals("CANCELLED",a.orders.get(o.getId()).getStatus());
        assertEquals(10,a.stock.forProduct(a.product.getId()).getQuantity());
        assertEquals(0,a.stock.forProduct(a.product.getId()).getReserved());
        assertEquals("FAILED",a.payments.forOrder(o.getId()).orElseThrow().getStatus());
        assertTrue(new JsonLineStore<>(a.dir,"orders/confirmed/confirmed.txt",Order.class).find(o.getId()).isEmpty());
    }

    @Test void stockCreationDoesNotTrustClientSideReservations() throws Exception {
        var a=setup(false);
        var second=new Product();second.setName("Pears");second.setCategory("fruits");
        second.setDescription("Fresh pears");second.setUnit("1 kg");second.setPrice(new BigDecimal("400.00"));
        second.setActive(true);second=a.products.create(second);
        Stock input=new Stock();input.setProductId(second.getId());input.setQuantity(10);input.setReserved(9);
        assertEquals(0,a.stock.create(input).getReserved());
        assertEquals(10,a.stock.available(second.getId()));
    }

    @Test void damagedFileMustNeverBeSilentlyOverwritten() throws Exception {
        var a=setup(false);
        Path productFile=Path.of(a.dir,"products/catalogue/products.txt");
        Files.writeString(productFile,"broken json\n");
        assertThrows(IllegalStateException.class,() -> a.products.create(a.product));
        assertEquals("broken json\n",Files.readString(productFile));
    }
}
