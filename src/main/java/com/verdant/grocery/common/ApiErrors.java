package com.verdant.grocery.common;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestControllerAdvice public class ApiErrors {
  @ExceptionHandler(ApiException.class) ResponseEntity<?> handle(ApiException e){return ResponseEntity.status(e.getStatus()).body(Map.of("error",e.getMessage()));}
  @ExceptionHandler({IllegalArgumentException.class}) ResponseEntity<?> invalid(Exception e){return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()==null?"Invalid request":e.getMessage()));}
  @ExceptionHandler(Exception.class) ResponseEntity<?> other(Exception e){
    System.err.println("Request failed: "+e.getClass().getSimpleName()+": "+e.getMessage());
    return ResponseEntity.status(500).body(Map.of("error","Unexpected server error. Please retry."));
  }
}
