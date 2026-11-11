package com.verdant.grocery.payments.strategy;
import org.springframework.stereotype.Component;
/**
 * Owner: Saraniha — payment strategy for cash collection after delivery.
 * A new COD order starts with PENDING payment, not PAID.
 */
@Component public class CashOnDeliveryProcessor implements PaymentProcessor {
 @Override public String method(){return "COD";}
 @Override public boolean authorize(String ref){return false;} // Admin records collection separately.
}
