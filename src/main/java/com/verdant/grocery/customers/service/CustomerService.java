package com.verdant.grocery.customers.service;
import com.verdant.grocery.customers.model.Customer;
import com.verdant.grocery.customers.model.AddressRecord;
import com.verdant.grocery.common.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
/**
 * Owner: Vakeesan — customer and account management (CRUD).
 * BCrypt hashes passwords; email is normalized before checking duplicates.
 * Updates preserve the stored role and hash so clients cannot grant admin access.
 */
// Owner: Vakeesan. Customer registration, update, search, deletion; password hashes, encapsulation.
@Service public class CustomerService extends CrudService<Customer> {
  private final BCryptPasswordEncoder encoder=new BCryptPasswordEncoder();
  private final JsonLineStore<AddressRecord> addresses;
  public CustomerService(@Value("${app.data-dir:data}") String dir){super(new JsonLineStore<>(dir,"customers/accounts/customers.txt",Customer.class));addresses=new JsonLineStore<>(dir,"customers/addresses/addresses.txt",AddressRecord.class);}
  protected String prefix(){return "CUS";}
  protected void validate(Customer v){
    v.setFullName(Checks.required(v.getFullName(),"Full name"));v.setEmail(Checks.email(v.getEmail()));
    v.setMobile(Checks.mobile(v.getMobile()));v.setAddress(Checks.required(v.getAddress(),"Address"));
  }
  public synchronized Customer register(String name,String email,String mobile,String address,String password,String confirm){
    if(password==null||password.length()<8)throw ApiException.bad("Password must have at least 8 characters");
    if(!password.equals(confirm))throw ApiException.bad("Passwords do not match");
    Customer c=new Customer(name,email);c.setMobile(mobile);c.setAddress(address);c.setRole("CUSTOMER");c.setActive(true);
    // Store a slow salted hash, never a plaintext password.
    c.setPasswordHash(encoder.encode(password));return create(c);
  }
  @Override public synchronized Customer create(Customer c){
    validate(c);if(all().stream().anyMatch(a->a.getEmail().equals(c.getEmail())))throw ApiException.conflict("Email already registered");
    if(all().stream().anyMatch(a->a.getMobile().equals(c.getMobile())))throw ApiException.conflict("Mobile number already registered");
    if(c.getPasswordHash()==null||c.getPasswordHash().isBlank())throw ApiException.bad("Password required");
    if(c.getRole()==null)c.setRole("CUSTOMER");Customer created=super.create(c);addresses.save(new AddressRecord(created.getId(),created.getAddress()));return created;
  }
  @Override public synchronized Customer update(String id,Customer next){
    Customer old=get(id);if("ADMIN".equals(old.getRole()))throw ApiException.bad("Cannot change admin via customer management");
    validate(next);if(all().stream().anyMatch(c->!id.equals(c.getId())&&c.getEmail().equals(next.getEmail())))throw ApiException.conflict("Email already registered");
    if(all().stream().anyMatch(c->!id.equals(c.getId())&&c.getMobile().equals(next.getMobile())))throw ApiException.conflict("Mobile number already registered");
    // A profile edit must not overwrite a password hash or elevate the account role.
    // The edit form does not contain the active flag. Preserve it on every update.
    next.setActive(old.getActive());next.setPasswordHash(old.getPasswordHash());next.setRole(old.getRole());Customer changed=super.update(id,next);addresses.save(new AddressRecord(changed.getId(),changed.getAddress()));return changed;
  }
  public synchronized Customer modifyProfile(String id,String name,String email,String mobile,String address){
    Customer c=get(id);Customer updated=new Customer();updated.setFullName(name);updated.setEmail(email);updated.setMobile(mobile);updated.setAddress(address);updated.setActive(c.getActive());return update(id,updated);
  }
  public Optional<Customer> findEmail(String email){return all().stream().filter(c->c.getEmail().equalsIgnoreCase(email)).findFirst();}
  public boolean authenticate(Customer c,String password){return c.getActive()&&encoder.matches(password==null?"":password,c.getPasswordHash());}
  public synchronized Customer seedAdmin(String email,String password){
    return findEmail(email).orElseGet(()->{Customer a=new Customer("Store Administrator",email);a.setMobile("0770000000");a.setAddress("Local demonstration");a.setRole("ADMIN");a.setActive(true);a.setPasswordHash(encoder.encode(password));return create(a);});
  }
  @Override public synchronized void delete(String id){super.delete(id);if(addresses.find(id).isPresent())addresses.remove(id);}
  public Map<String,Object> safe(Customer c){return Map.of("id",c.getId(),"fullName",c.getFullName(),"email",c.getEmail(),"mobile",c.getMobile(),"address",c.getAddress(),"role",c.getRole(),"active",c.getActive());}
}
