package com.acmeair.client.fallback;

import com.acmeair.client.responses.CarResponse;
import jakarta.enterprise.context.Dependent;
import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;

import java.util.logging.Logger;

@Dependent
public class CarFallbackHandler implements FallbackHandler<CarResponse> {
    protected static Logger logger =  Logger.getLogger(CarFallbackHandler.class.getName());

    @Override
    public CarResponse handle(ExecutionContext context) {
        System.out.println("Car Service Call Failed - check connection to Car Service.");
        logger.info("fallback for " + context.getMethod().getName());
        return null;
    }
}
