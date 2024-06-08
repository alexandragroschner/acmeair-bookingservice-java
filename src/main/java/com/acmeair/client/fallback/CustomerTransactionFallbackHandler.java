package com.acmeair.client.fallback;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;

import java.util.logging.Logger;

@ApplicationScoped
public class CustomerTransactionFallbackHandler implements FallbackHandler<Response> {
    protected static Logger logger =  Logger.getLogger(CustomerFallbackHandler.class.getName());

    @Override
    public Response handle(ExecutionContext context) {
        System.out.println("Customer Call for database transaction failed - check connection to Customer Service.");
        logger.info("fallback for " + context.getMethod().getName());
        return null;
    }
}
