package com.verdant.grocery.customers.service;
import com.verdant.grocery.customers.model.Feedback;
import com.verdant.grocery.common.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
/**
 * Owner: Vakeesan — validated customer feedback (CRUD).
 * A rating must be 1–5; customer-facing ownership checks are done in controller.
 */
// Owner: Vakeesan. Customer ratings 1–5 plus store replies.
@Service public class FeedbackService extends CrudService<Feedback>{
  public FeedbackService(@Value("${app.data-dir:data}")String dir){super(new JsonLineStore<>(dir,"customers/feedback/feedback.txt",Feedback.class));}
  protected String prefix(){return "FDB";}
  protected void validate(Feedback f){Checks.required(f.getCustomerId(),"Customer");if(f.getRating()<1||f.getRating()>5)throw ApiException.bad("Rating must be 1–5");f.setComment(Checks.required(f.getComment(),"Comment"));}
}
