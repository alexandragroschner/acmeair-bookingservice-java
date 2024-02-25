package com.acmeair.web;


import com.acmeair.client.*;
import com.acmeair.service.BookingService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

@ApplicationScoped
public class RewardTracker {

  @Inject
  BookingService bs;

  @Inject @RestClient
  private CustomerClient customerClient;

  @Inject @RestClient
  private FlightClient flightClient;

  @Inject @RestClient
  private RewardClient rewardClient;

  @Inject @RestClient
  private CarClient carClient;

  private AtomicLong customerSuccesses = new AtomicLong(0);
  private AtomicLong flightSuccesses = new AtomicLong(0);
  private AtomicLong customerFailures = new AtomicLong(0);
  private AtomicLong flightFailures = new AtomicLong(0);
  private static final Logger logger = Logger.getLogger(RewardTracker.class.getName());

  public Long updateRewardMiles(String userid, String flightSegId, String flightId, boolean add)  {

    CostAndMilesResponse costAndMiles = flightClient.getCostAndMiles(flightId);
    logger.warning("costAndMiles requested");
    CarResponse testCar = carClient.getCarByName("trabant");
    if (testCar == null) {
      logger.warning("testCar is null");
    }

    logger.warning("CALLING CAR SERVICE: " + testCar.getId() + " " + testCar.getCarName());
   
    if (costAndMiles == null ) {
      // flight call failed, return null
      flightFailures.incrementAndGet();
      return null;
    }

    flightSuccesses.incrementAndGet();
    if (!add ) {
      costAndMiles.setMiles((costAndMiles.getMiles()) * -1);
    }

    // TODO: implement method to get current miles instead of total miles (without hack)
    //HACK: pass 0 miles to customerClient.updateCustomerTotalMiles();
    Long currentMiles = customerClient.updateCustomerTotalMiles(userid, 0L).getMiles();
    logger.warning("Current miles: " + currentMiles);
    Long totalMiles = customerClient.updateCustomerTotalMiles(userid, costAndMiles.getMiles()).getMiles();
    logger.warning("Total miles: " + totalMiles);

    // pass flight miles, current miles and cost to reward service
    PriceResponse newPrice = rewardClient.getNewPrice(costAndMiles.getMiles().toString(), currentMiles.toString(), costAndMiles.getCost().toString());
    logger.warning("new price is" + newPrice.getPrice());


    if (totalMiles == null) {
      // customer call failed, return null
      customerFailures.incrementAndGet();
      return null;
    }

    // Both calls succeeded!
    customerSuccesses.incrementAndGet();   
    return newPrice.getPrice();
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

  //USER ADDED CODE:

  public Long updateRewardMiles(String userid, String flightSegId, String flightId,
                                String retFlightSegId, String retFlightId, boolean add)  {

    CostAndMilesResponse costAndMiles = flightClient.getCostAndMiles(flightId);
    CostAndMilesResponse retCostAndMiles = flightClient.getCostAndMiles(retFlightId);

    logger.warning("costAndMiles requested");
    CarResponse testCar = carClient.getCarByName("trabant");
    if (testCar == null) {
      logger.warning("testCar is null");
    }

    logger.warning("CALLING CAR SERVICE: " + testCar.getId() + " " + testCar.getCarName());

    if (costAndMiles == null ) {
      // flight call failed, return null
      flightFailures.incrementAndGet();
      return null;
    }

    flightSuccesses.incrementAndGet();
    if (!add ) {
      costAndMiles.setMiles((costAndMiles.getMiles()) * -1);
    }

    // TODO: implement method to get current miles instead of total miles (without hack)
    //HACK: pass 0 miles to customerClient.updateCustomerTotalMiles();
    Long currentMiles = customerClient.updateCustomerTotalMiles(userid, 0L).getMiles();
    logger.warning("Current miles: " + currentMiles);
    Long totalFlightMiles = costAndMiles.getMiles() + retCostAndMiles.getMiles();
    Long totalMiles = customerClient.updateCustomerTotalMiles(userid, totalFlightMiles).getMiles();
    logger.warning("Total miles: " + totalMiles);
    Long totalFlightPrice = costAndMiles.getCost() + retCostAndMiles.getCost();

    // pass flight miles, current miles and cost to reward service
    PriceResponse newPrice = rewardClient.getNewPrice(totalFlightMiles.toString(), currentMiles.toString(), totalFlightPrice.toString());
    logger.warning("new price is" + newPrice.getPrice());


    if (totalMiles == null) {
      // customer call failed, return null
      customerFailures.incrementAndGet();
      return null;
    }

    // Both calls succeeded!
    customerSuccesses.incrementAndGet();
    return newPrice.getPrice();
  }
}
