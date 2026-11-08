package com.verdant.grocery.common;
import java.math.BigDecimal;
import java.util.regex.Pattern;
/**
 * Common server-side validation. Browser form validation is not trusted because
 * HTTP clients can bypass it. Money is rounded as BigDecimal, not double.
 */
public final class Checks {
  private Checks(){}
  private static final Pattern EMAIL=Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
  public static String required(String v,String name){if(v==null||v.isBlank())throw ApiException.bad(name+" is required");return v.trim();}
  public static String email(String v){String x=required(v,"Email").toLowerCase(java.util.Locale.ROOT);if(!EMAIL.matcher(x).matches())throw ApiException.bad("Invalid email address");return x;}
  public static String mobile(String v){String x=required(v,"Mobile").replace(" ","").replace("-","");if(x.startsWith("+94"))x="0"+x.substring(3);if(!x.matches("07[0-9]{8}"))throw ApiException.bad("Enter a valid Sri Lankan mobile number");return x;}
  public static int nonnegative(int n,String name){if(n<0)throw ApiException.bad(name+" cannot be negative");return n;}
  public static BigDecimal positive(BigDecimal n,String name){if(n==null||n.signum()<=0)throw ApiException.bad(name+" must be greater than zero");return n.setScale(2,java.math.RoundingMode.HALF_UP);}
  public static String id(String prefix){return prefix+"-"+java.util.UUID.randomUUID().toString().substring(0,8).toUpperCase();}
}
