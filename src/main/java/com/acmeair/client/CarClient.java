package com.acmeair.client;

import com.acmeair.client.fallback.CarFallbackHandler;
import com.acmeair.client.responses.CarResponse;
import jakarta.ws.rs.*;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.io.IOException;
import java.time.temporal.ChronoUnit;

@RegisterRestClient(configKey="carClient")
@Path("/")
public interface CarClient {
    @GET
    @Path("/byname/{name}")
    //@Consumes("text/plain")
    @Produces("application/json")
    @Timeout(10000) // throws exception after 500 ms which invokes fallback handler
    @CircuitBreaker(requestVolumeThreshold=4,failureRatio=0.5,successThreshold=10,delay=1,delayUnit= ChronoUnit.SECONDS)
    @Retry(maxRetries=3,delayUnit=ChronoUnit.SECONDS,delay=5,durationUnit=ChronoUnit.SECONDS,
            maxDuration=30, retryOn = Exception.class, abortOn = IOException.class)
    @Fallback(CarFallbackHandler.class)
    public CarResponse getCarByName(@PathParam("name") String carName);
}
