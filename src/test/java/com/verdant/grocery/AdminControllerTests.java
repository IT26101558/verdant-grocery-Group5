
package com.verdant.grocery;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import java.nio.file.*;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AdminControllerTests {

  private static final Path DATA;

  static {
    try {
      DATA = Files.createTempDirectory("verdant-admin-test-");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @DynamicPropertySource
  static void data(DynamicPropertyRegistry p) {
    p.add("app.data-dir", DATA::toString);
  }

  @Autowired
  MockMvc mvc;

  @Autowired
  ObjectMapper json;

  private MockHttpSession admin() throws Exception {
    var result = mvc.perform(
            post("/api/auth/admin-login")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"admin@grocery.local\",\"password\":\"GroceryDemo@2026\"}")
    ).andExpect(status().isOk()).andReturn();

    return (MockHttpSession) result.getRequest().getSession(false);
  }

  private JsonNode request(
          String method,
          String url,
          String body,
          MockHttpSession s,
          int expected
  ) throws Exception {

    var b = switch (method) {
      case "POST" -> post(url);
      case "PUT" -> put(url);
      case "DELETE" -> delete(url);
      default -> get(url);
    };

    var result = mvc.perform(
            b.session(s)
                    .header("X-Requested-With", "XMLHttpRequest")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body == null ? "" : body)
    ).andReturn();

    int actual = result.getResponse().getStatus();
    String response = result.getResponse().getContentAsString();

    System.out.println("API: " + method + " " + url);
    System.out.println("Expected: " + expected);
    System.out.println("Actual: " + actual);

    if (actual != expected) {
      System.out.println("Response: " + response);
    }

    assertEquals(expected, actual,
            "Failed API: " + method + " " + url + " | Response: " + response);

    return json.readTree(response);
  }

  @Test
  void adminProductAndCustomerFullFlow() throws Exception {

    MockHttpSession s = admin();

    String email = "visitor" + System.nanoTime() + "@example.com";

    String mobile = "07" + String.format(
            "%08d",
            Math.floorMod(System.nanoTime(), 100000000L)
    );

    var c = request(
            "POST",
            "/api/admin/customers",
            "{\"fullName\":\"Demo Client\",\"email\":\"" + email +
                    "\",\"mobile\":\"" + mobile +
                    "\",\"address\":\"Kandy\",\"password\":\"testpass123\"}",
            s,
            200
    );

    String cid = c.get("id").asText();

    request("GET", "/api/admin/customers/" + cid, null, s, 200);

    request(
            "PUT",
            "/api/admin/customers/" + cid,
            "{\"fullName\":\"Changed Client\",\"email\":\"" + email +
                    "\",\"mobile\":\"" + mobile +
                    "\",\"address\":\"Kandy\"}",
            s,
            200
    );

    request("DELETE", "/api/admin/customers/" + cid, null, s, 200);

    var p = request(
            "POST",
            "/api/admin/products",
            "{\"name\":\"Test Mango\",\"category\":\"fruits\",\"description\":\"Delicious mango\",\"unit\":\"1 kg\",\"price\":250}",
            s,
            200
    );

    String id = p.get("id").asText();

    request("GET", "/api/admin/products/" + id, null, s, 200);

    request(
            "PUT",
            "/api/admin/products/" + id,
            "{\"name\":\"Test Mango\",\"category\":\"fruits\",\"description\":\"Delicious mango\",\"unit\":\"1 kg\",\"price\":300}",
            s,
            200
    );

    request("DELETE", "/api/admin/products/" + id, null, s, 200);

    request("GET", "/api/admin/products/" + id, null, s, 200);
  }

  @Test
  void remainingAdminAreasCreateReadEditAndDelete() throws Exception {

    MockHttpSession s = admin();

    var p = request(
            "POST",
            "/api/admin/products",
            "{\"name\":\"Sample test pear\",\"category\":\"fruits\",\"description\":\"Test produce\",\"unit\":\"1 kg\",\"price\":500}",
            s,
            200
    );

    String pid = p.get("id").asText();

    var stock = request(
            "POST",
            "/api/admin/inventory",
            "{\"productId\":\"" + pid + "\",\"quantity\":0,\"reorderLevel\":2}",
            s,
            200
    );

    String sid = stock.get("id").asText();

    request("GET", "/api/admin/inventory/" + sid, null, s, 200);

    request(
            "PUT",
            "/api/admin/inventory/" + sid,
            "{\"quantity\":0,\"reorderLevel\":3}",
            s,
            200
    );

    request("DELETE", "/api/admin/inventory/" + sid, null, s, 200);

    String email = "grocery-test-" + System.nanoTime() + "@example.com";

    request(
            "POST",
            "/api/auth/register",
            "{\"fullName\":\"Demo Buyer\",\"email\":\"" + email +
                    "\",\"mobile\":\"0771234567\",\"address\":\"Colombo\",\"password\":\"testpass123\",\"confirmPassword\":\"testpass123\"}",
            s,
            200
    );

    var result = mvc.perform(
            post("/api/auth/login")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"" + email + "\",\"password\":\"testpass123\"}")
    ).andExpect(status().isOk()).andReturn();

    MockHttpSession buyer =
            (MockHttpSession) result.getRequest().getSession(false);

    var catalog = request("GET", "/api/catalog/products", null, s, 200);

    String stockedProduct = catalog.get(0).get("id").asText();

    request(
            "POST",
            "/api/cart/items",
            "{\"productId\":\"" + stockedProduct + "\",\"quantity\":1}",
            buyer,
            200
    );

    var order = request(
            "POST",
            "/api/checkout/place",
            "{\"method\":\"DEMO_CARD\",\"key\":\"" + System.nanoTime() + "\"}",
            buyer,
            200
    );

    String oid = order.get("id").asText();

    request("GET", "/api/admin/orders/" + oid, null, s, 200);

    request(
            "PUT",
            "/api/admin/orders/" + oid,
            "{\"address\":\"Kandy\"}",
            s,
            200
    );

    request("DELETE", "/api/admin/orders/" + oid, null, s, 200);

    String cid = request(
            "GET",
            "/api/auth/me",
            null,
            buyer,
            200
    ).get("id").asText();

    var cod = request(
            "POST",
            "/api/admin/orders",
            "{\"customerId\":\"" + cid +
                    "\",\"productId\":\"" + stockedProduct +
                    "\",\"quantity\":1}",
            s,
            200
    );

    String codId = cod.get("id").asText();

    var deliveries = request(
            "GET",
            "/api/admin/deliveries",
            null,
            s,
            200
    );

    String did = null;

    for (var d : deliveries) {
      if (d.get("orderId").asText().equals(codId)) {
        did = d.get("id").asText();
      }
    }

    assertNotNull(did);

    request("GET", "/api/admin/deliveries/" + did, null, s, 200);

    request(
            "PUT",
            "/api/admin/deliveries/" + did,
            "{\"driver\":\"Demo Courier\",\"status\":\"PACKED\",\"estimatedDate\":\"2026-11-02\"}",
            s,
            200
    );

    request("DELETE", "/api/admin/deliveries/" + did, null, s, 200);

    var nextDelivery = request(
            "POST",
            "/api/admin/deliveries",
            "{\"orderId\":\"" + codId +
                    "\",\"driver\":\"Local Courier\",\"status\":\"PROCESSING\"}",
            s,
            200
    );

    assertEquals(codId, nextDelivery.get("orderId").asText());

    var payments = request(
            "GET",
            "/api/admin/payments",
            null,
            s,
            200
    );

    String payId = null;

    for (var pay : payments) {
      if (pay.get("orderId").asText().equals(codId)
              && !pay.get("archived").asBoolean()) {
        payId = pay.get("id").asText();
      }
    }

    assertNotNull(payId);

    request("GET", "/api/admin/payments/" + payId, null, s, 200);

    request("DELETE", "/api/admin/payments/" + payId, null, s, 200);

    var replacement = request(
            "POST",
            "/api/admin/payments",
            "{\"orderId\":\"" + codId + "\"}",
            s,
            200
    );

    assertEquals("PAID", replacement.get("status").asText());
  }

  @Test
  void anonymousAndCustomerCannotAccessAdmin() throws Exception {

    mvc.perform(
            get("/api/admin/overview")
    ).andExpect(status().isUnauthorized());

    mvc.perform(
            post("/api/auth/login")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"admin@grocery.local\",\"password\":\"GroceryDemo@2026\"}")
    ).andExpect(status().isUnauthorized());
  }
}
