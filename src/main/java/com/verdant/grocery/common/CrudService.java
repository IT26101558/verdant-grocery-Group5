package com.verdant.grocery.common;
import java.util.*;
/**
 * Shared abstract service: generic CRUD demonstrates inheritance and abstraction.
 * Each member's concrete service supplies an ID prefix and domain validation.
 */
// Standard create/read/update/delete implementation used by owned domain services.
public abstract class CrudService<T extends BaseRecord> {
  protected final JsonLineStore<T> store;
  protected CrudService(JsonLineStore<T> store){this.store=store;}
  public List<T> all(){return store.all();}
  public T get(String id){return store.require(id);}
  public synchronized T create(T obj){validate(obj);obj.setId(Checks.id(prefix()));store.save(obj);return obj;}
  public synchronized T update(String id,T obj){T old=get(id);validate(obj);obj.setId(id);store.save(obj);return obj;}
  public synchronized void delete(String id){store.remove(id);}
  protected abstract String prefix();
  protected abstract void validate(T obj);
}
