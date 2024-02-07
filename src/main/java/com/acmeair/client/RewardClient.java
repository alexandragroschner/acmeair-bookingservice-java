package com.acmeair.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.io.IOException;
import java.time.temporal.ChronoUnit;

@RegisterRestClient(configKey="rewardClient")
@Path("/")
public interface RewardClient {
    @GET
    @Path("/newprice")
    @Consumes("text/plain")
    @Produces("application/json")
    @Timeout(10000) // throws exception after 500 ms which invokes fallback handler
    @CircuitBreaker(requestVolumeThreshold=4,failureRatio=0.5,successThreshold=10,delay=1,delayUnit= ChronoUnit.SECONDS)
    @Retry(maxRetries=3,delayUnit=ChronoUnit.SECONDS,delay=5,durationUnit=ChronoUnit.SECONDS,
            maxDuration=30, retryOn = Exception.class, abortOn = IOException.class)
    @Fallback(RewardFallbackHandler.class)
    public PriceResponse getNewPrice(
            @QueryParam("flightmiles") String flightMiles,
            @QueryParam("customermiles") String customerMiles,
            @QueryParam("cost") String baseCost);
}
