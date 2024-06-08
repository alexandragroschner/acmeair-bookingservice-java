/*******************************************************************************
 * Copyright (c) 2018 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package com.acmeair.client;

import com.acmeair.client.fallback.CustomerFallbackHandler;
import com.acmeair.client.fallback.CustomerTransactionFallbackHandler;
import com.acmeair.client.responses.CustomerMilesResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.io.IOException;
import java.time.temporal.ChronoUnit;

@RegisterRestClient(configKey="customerClient")
@Path("/")
public interface CustomerClient {

  @POST
  @Path("/internal/updateCustomerTotalMiles/{custid}")
  @Consumes({ "application/x-www-form-urlencoded" })
  @Produces("application/json")
  @Timeout(10000) // throws exception after 500 ms which invokes fallback handler
  @CircuitBreaker(requestVolumeThreshold=4,failureRatio=0.5,successThreshold=10,delay=1,delayUnit=ChronoUnit.SECONDS)
  @Retry(maxRetries=3,delayUnit=ChronoUnit.SECONDS,delay=5,durationUnit=ChronoUnit.SECONDS,
    maxDuration=30, retryOn = Exception.class, abortOn = IOException.class)
  @Fallback(CustomerFallbackHandler.class)
  public CustomerMilesResponse updateCustomerTotalMiles(
      @PathParam("custid") String customerid,
      @FormParam("miles") Long miles,
      @FormParam("loyalty") Long loyaltyPoints);

  @GET
  @Path("/internal/getCustomerTotalMiles")
  @Consumes({ "application/x-www-form-urlencoded" })
  @Produces("application/json")
  @Timeout(10000) // throws exception after 500 ms which invokes fallback handler
  @CircuitBreaker(requestVolumeThreshold=4,failureRatio=0.5,successThreshold=10,delay=1,delayUnit=ChronoUnit.SECONDS)
  @Retry(maxRetries=3,delayUnit=ChronoUnit.SECONDS,delay=5,durationUnit=ChronoUnit.SECONDS,
          maxDuration=30, retryOn = Exception.class, abortOn = IOException.class)
  @Fallback(CustomerFallbackHandler.class)
  public CustomerMilesResponse getCustomerTotalMiles(@QueryParam("custid") String customerid);

  @POST
  @Path("/internal/abort")
  @Consumes({"application/json"})
  @Produces("application/json")
  @Timeout(10000) // throws exception after 500 ms which invokes fallback handler
  @CircuitBreaker(requestVolumeThreshold=4,failureRatio=0.5,successThreshold=10,delay=1,delayUnit=ChronoUnit.SECONDS)
  @Retry(maxRetries=3,delayUnit=ChronoUnit.SECONDS,delay=5,durationUnit=ChronoUnit.SECONDS,
          maxDuration=30, retryOn = Exception.class, abortOn = IOException.class)
  @Fallback(CustomerTransactionFallbackHandler.class)
  public Response abortMongo(String mongoSessionId);


  @POST
  @Path("/internal/commit")
  @Consumes({"application/json"})
  @Produces("application/json")
  @Timeout(10000) // throws exception after 500 ms which invokes fallback handler
  @CircuitBreaker(requestVolumeThreshold=4,failureRatio=0.5,successThreshold=10,delay=1,delayUnit=ChronoUnit.SECONDS)
  @Retry(maxRetries=3,delayUnit=ChronoUnit.SECONDS,delay=5,durationUnit=ChronoUnit.SECONDS,
          maxDuration=30, retryOn = Exception.class, abortOn = IOException.class)
  @Fallback(CustomerTransactionFallbackHandler.class)
  public Response commitMongo(String mongoSessionId);
}

