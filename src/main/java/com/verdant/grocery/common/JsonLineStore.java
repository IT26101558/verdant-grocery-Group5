package com.verdant.grocery.common;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.springframework.beans.factory.annotation.Value;
/**
 * Shared file repository for JSON-lines records (file read/write persistence).
 * Read/write locks prevent two requests in this JVM from overwriting each other.
 * A temporary file and rename reduce the chance of leaving a partly written file.
 * File operations are NOT an ACID transaction across multiple folders.
 */
// JSON-per-line *.txt persistence. One type per file; atomic replacement of a whole file.
public class JsonLineStore<T extends BaseRecord> {
  private final Path path;
  private final Class<T> type;
  private final ObjectMapper mapper=new ObjectMapper().registerModule(new JavaTimeModule());
  private final ReentrantReadWriteLock lock=new ReentrantReadWriteLock();
  public JsonLineStore(String dataDir,String relative,Class<T> type){this.path=Path.of(dataDir).resolve(relative);this.type=type;}
  public List<T> all(){lock.readLock().lock();try{
    if(!Files.exists(path))return new ArrayList<>();
    List<T> items=new ArrayList<>();int lineNo=0;
    // Fail closed on malformed stored records: silently skipping them would
    // erase the damaged lines during the next whole-file update.
    for(String s:Files.readAllLines(path)){lineNo++;if(s.isBlank())continue;
      try{items.add(mapper.readValue(s,type));}
      catch(Exception malformed){throw new IllegalStateException("Invalid JSON in "+path.getFileName()+" at line "+lineNo,malformed);}}
    return items;
  }catch(IOException e){throw new IllegalStateException("Cannot read "+path,e);}finally{lock.readLock().unlock();}}
  public Optional<T> find(String id){return all().stream().filter(v->id!=null&&id.equals(v.getId())).findFirst();}
  public T require(String id){return find(id).orElseThrow(()->ApiException.notFound("Record not found: "+id));}
  public void save(T record){lock.writeLock().lock();try{List<T> a=all();a.removeIf(t->t.getId().equals(record.getId()));a.add(record);write(a);}finally{lock.writeLock().unlock();}}
  public void remove(String id){lock.writeLock().lock();try{List<T>a=all();boolean had=a.removeIf(t->t.getId().equals(id));if(!had)throw ApiException.notFound("Record not found");write(a);}finally{lock.writeLock().unlock();}}
  public void write(List<T> data){lock.writeLock().lock();try{
    Files.createDirectories(path.getParent());
    Path temp=Files.createTempFile(path.getParent(),".tmp-",".txt");
    try{
      try(BufferedWriter out=Files.newBufferedWriter(temp)){
        for(T record:data){out.write(mapper.writeValueAsString(record));out.newLine();}
      }
      // Replace only after the complete new file is written.
      try{Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
      catch(AtomicMoveNotSupportedException e){Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING);}
    }finally{Files.deleteIfExists(temp);}
  }catch(IOException e){throw new IllegalStateException("Cannot save "+path,e);}finally{lock.writeLock().unlock();}}
}
