package com.verdant.grocery.payments.strategy;
import org.springframework.stereotype.Component;
/**
 * Owner: Saraniha — test-only simulated card authorization strategy.
 * The code intentionally uses no real card number, expiry, or security code.
 */
@Component public class DemoCardProcessor implements PaymentProcessor {
 @Override public String method(){return "DEMO_CARD";}
 @Override public boolean authorize(String ref){return "SYNTHETIC-TEST-APPROVE".equals(ref);}
}
