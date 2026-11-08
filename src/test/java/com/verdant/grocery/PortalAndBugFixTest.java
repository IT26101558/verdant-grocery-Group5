package com.verdant.grocery;

import com.verdant.grocery.common.ApiException;
import com.verdant.grocery.customers.service.CustomerService;
import com.verdant.grocery.security.SecurityGate;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.*;

/** Regression checks for portal role boundaries and the profile edit bug. */
class PortalAndBugFixTest {
    @Test
    void customerProfileEditKeepsLoginActiveAndRole() throws Exception {
        var service=new CustomerService(Files.createTempDirectory("customer-profile-").toString());
        var customer=service.register("Example Customer","first@example.com","0771234567","Colombo","strongpass123","strongpass123");
        var changed=service.modifyProfile(customer.getId(),"New Name","new@example.com","0771234567","New Address");
        assertTrue(changed.getActive());
        assertEquals("CUSTOMER",changed.getRole());
        assertTrue(service.authenticate(changed,"strongpass123"));
        assertEquals("New Name",service.get(customer.getId()).getFullName());
    }

    @Test
    void duplicatePhoneIsRejected() throws Exception {
        var service=new CustomerService(Files.createTempDirectory("customer-phone-").toString());
        service.register("First","first@example.com","0771234567","Colombo","strongpass123","strongpass123");
        assertThrows(ApiException.class,()->service.register("Second","second@example.com","+94771234567","Jaffna","strongpass123","strongpass123"));
    }

    @Test
    void customerIsRedirectedFromAdminPortalAndDeniedAdminApi() throws Exception {
        var service=new CustomerService(Files.createTempDirectory("portal-roles-").toString());
        var customer=service.register("Test Person","test@example.com","0771234567","Colombo","strongpass123","strongpass123");
        var guard=new SecurityGate(service);
        var session=new MockHttpSession();
        session.setAttribute("customerId",customer.getId());
        session.setAttribute("role","CUSTOMER");
        var page=new MockHttpServletRequest("GET","/admin-portal.html");
        page.setSession(session);
        var response=new MockHttpServletResponse();
        assertFalse(guard.preHandle(page,response,new Object()));
        assertEquals("/admin-login.html",response.getRedirectedUrl());
        var api=new MockHttpServletRequest("GET","/api/admin/overview");
        api.setSession(session);
        assertThrows(ApiException.class,()->guard.preHandle(api,new MockHttpServletResponse(),new Object()));
        var customerPage=new MockHttpServletRequest("GET","/customer-portal.html");
        customerPage.setSession(session);
        assertTrue(guard.preHandle(customerPage,new MockHttpServletResponse(),new Object()));
    }

    @Test
    void adminIsDeniedCustomerApi() throws Exception {
        var service=new CustomerService(Files.createTempDirectory("admin-roles-").toString());
        var admin=service.seedAdmin("admin@example.com","strongpass123");
        var guard=new SecurityGate(service);
        var session=new MockHttpSession();
        session.setAttribute("customerId",admin.getId());
        session.setAttribute("role","ADMIN");
        var api=new MockHttpServletRequest("GET","/api/customer/dashboard");
        api.setSession(session);
        assertThrows(ApiException.class,()->guard.preHandle(api,new MockHttpServletResponse(),new Object()));
    }
}
