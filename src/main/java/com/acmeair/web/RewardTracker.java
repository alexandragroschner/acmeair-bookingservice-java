package com.acmeair.web;


import com.acmeair.client.CostAndMilesResponse;
import com.acmeair.client.CustomerClient;
import com.acmeair.client.FlightClient;
import com.acmeair.service.BookingService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class RewardTracker {

  @Inject
  BookingService bs;

  @Inject @RestClient
  private CustomerClient customerClient;

  @Inject @RestClient
  private FlightClient flightClient;

  private AtomicLong customerSuccesses = new AtomicLong(0);
  private AtomicLong flightSuccesses = new AtomicLong(0);
  private AtomicLong customerFailures = new AtomicLong(0);
  private AtomicLong flightFailures = new AtomicLong(0);
    
  //TODO: For now, use the Fault Tolerance to make this done async.
  @Timeout(500) //throws a timeout exception if method does not return withing 400 ms
  @Asynchronous
  public CompletionStage<Long> updateRewardMiles(String userid, String flightSegId, String flightId, boolean add) throws InterruptedException  {
    //TODO: get base cost and miles: flightClient.getCostAndMiles(flightId).getMiles()/.getCost()
    CostAndMilesResponse costAndMiles = flightClient.getCostAndMiles(flightId);
    //Long miles = flightClient.getRewardMiles(flightSegId).getMiles();
   
    if (costAndMiles == null ) {
      // flight call failed, return null
      flightFailures.incrementAndGet();
      return CompletableFuture.completedFuture(null);
    }

    flightSuccesses.incrementAndGet();
    if (!add ) {
      costAndMiles.setMiles((costAndMiles.getMiles()) * -1);
    }

    //TODO: cimplement method to get current miles instead of total miles
    Long totalMiles = customerClient.updateCustomerTotalMiles(userid, costAndMiles.getMiles()).getMiles();

    //TODO: pass flight miles, current miles and cost to reward service
    //TODO: create rewardClient class

    if (totalMiles == null) {
      // customer call failed, return null
      customerFailures.incrementAndGet();
      return CompletableFuture.completedFuture(null);
    }

    // Both calls succeeded!
    customerSuccesses.incrementAndGet();   
    return CompletableFuture.completedFuture(totalMiles);
  }

  public long getCustomerSuccesses() {
    return customerSuccesses.get();
  }

  public long getFlightSucesses() {
    return flightSuccesses.get();
  }

  public long getCustomerFailures() {
    return customerFailures.get();
  }

  public long getFlightFailures() {
    return flightFailures.get();
  }
}
