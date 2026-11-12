package com.verdant.grocery.payments.strategy;
// Interface + runtime polymorphism. Online payments are DEMO ONLY, never real processing.
/**
 * Owner: Saraniha — payment strategy interface showing runtime polymorphism.
 * All methods are demonstration-only and must never charge a real card.
 */
public interface PaymentProcessor {String method();boolean authorize(String testReference);}
