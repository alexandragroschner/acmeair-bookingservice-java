package com.acmeair.web;


import com.acmeair.client.CarClient;
import com.acmeair.client.FlightClient;
import com.acmeair.client.responses.CarResponse;
import com.acmeair.client.responses.CostAndMilesResponse;
import com.acmeair.client.responses.CustomerMilesResponse;
import com.acmeair.client.responses.PriceResponse;
import com.acmeair.service.BookingService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

@ApplicationScoped
public class RewardTracker {

    private static final long MINIMUM_CAR_PRICE = 25;
    @Inject
    BookingService bs;

    @Inject
    @RestClient
    private FlightClient flightClient;

    @Inject
    @RestClient
    private CarClient carClient;

    private AtomicLong customerSuccesses = new AtomicLong(0);
    private AtomicLong flightSuccesses = new AtomicLong(0);
    private AtomicLong customerFailures = new AtomicLong(0);
    private AtomicLong flightFailures = new AtomicLong(0);
    private static final Logger logger = Logger.getLogger(RewardTracker.class.getName());

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
    public List<Long> updateRewardMiles(String userid, String flightId, String retFlightId, boolean add,
                                        String carName, boolean isOneWay) {

        // this will be the response and contain the updated flight price and updated car price (if no car -> null)
        List<Long> updatedPrices = new ArrayList<>();

        // gets miles and cost of chosen flight
        CostAndMilesResponse costAndMiles = flightClient.getCostAndMiles(flightId);
        CostAndMilesResponse retCostAndMiles;
        if (isOneWay) {
            // make an empty return miles response if no return flight
            logger.warning("Creating empty Return Flight CostAndMilesResponse due one way booking");
            retCostAndMiles = new CostAndMilesResponse(0L, 0L);
        } else {
            retCostAndMiles = flightClient.getCostAndMiles(retFlightId);
        }

        if (costAndMiles == null) {
            // flight call failed, return null
            flightFailures.incrementAndGet();
            return null;
        }
        logger.warning("costAndMiles requested");
        CarResponse carToBook = null;

        if (Objects.nonNull(carName)) {
            carToBook = carClient.getCarByName(carName);
            logger.warning("Called car service for car: " + carName);
            if (carToBook == null) {
                logger.warning("carToBook is null - could not find car with name: " + carName);
                return null;
            }
        }

        flightSuccesses.incrementAndGet();
        //subtract miles if booking is cancelled
        if (!add) {
            costAndMiles.setMiles((costAndMiles.getMiles()) * -1);
        }

        CustomerMilesResponse currentMilesAndLoyalty = bs.getCurrentMilesAndPoints(userid);
        logger.warning("Current miles: " + currentMilesAndLoyalty.getMiles());
        logger.warning("Current loyalty: " + currentMilesAndLoyalty.getLoyaltyPoints());
        Long totalFlightMiles = costAndMiles.getMiles() + retCostAndMiles.getMiles();
        Long totalFlightPrice = costAndMiles.getCost() + retCostAndMiles.getCost();

        // pass flight miles, current miles and cost to reward service
        PriceResponse newFlightPrice = getNewFlightPrice(totalFlightMiles, currentMilesAndLoyalty.getMiles(), totalFlightPrice);
        updatedPrices.add(newFlightPrice.getPrice());

        //get new car price
        Long loyaltyPoints = 0L;
        if (Objects.nonNull(carToBook)) {
            PriceResponse newCarPrice = getNewCarPrice(carToBook.getLoyaltyPoints(),
                    currentMilesAndLoyalty.getLoyaltyPoints(), (long) carToBook.getBaseCost());

            logger.warning("new car price is " + newCarPrice.getPrice());
            loyaltyPoints = carToBook.getLoyaltyPoints();
            updatedPrices.add(newCarPrice.getPrice());
        } else {
            // add 0 as car price if no car is booked
            logger.warning("adding 0 as car price (no car booked)");
            updatedPrices.add(0L);
        }

        logger.warning("new flight price is " + newFlightPrice.getPrice());

        if (!add) {
            loyaltyPoints = loyaltyPoints * -1;
        }
        CustomerMilesResponse updatedMilesAndLoyalty = bs.updateCustomerMilesAndPoints(userid, totalFlightMiles, loyaltyPoints);
        logger.warning("Updated miles: " + updatedMilesAndLoyalty.getMiles());
        logger.warning("Updated loyalty: " + updatedMilesAndLoyalty.getLoyaltyPoints());

        // Both calls succeeded!
        customerSuccesses.incrementAndGet();
        return updatedPrices;
    }

    private PriceResponse getNewCarPrice(Long carLoyalty, Long customerLoyalty, Long carBaseCost) {
        Long pointsToCheck = customerLoyalty + carLoyalty;
        logger.warning("pointsToCheck: " + pointsToCheck);

        List<Integer> rewardLevels = bs.getCarRewardMapping();
        // for every id (miles) check if miles of customer are in that status level
        // if yes, get reductionPercentage for that status
        int lastId = 0;
        for (Integer id : rewardLevels) {
            if (pointsToCheck < id) {
                // return baseCost multiplied by reductionPercentage
                return new PriceResponse(adjustCarPrice(id, carBaseCost));
            }
            // this happens only when id is not smaller than the miles to check i.e. is the highest id
            lastId = id;
        }
        return new PriceResponse(adjustCarPrice(lastId, carBaseCost));
    }

    public PriceResponse getNewFlightPrice(Long flightMiles, Long customerMiles, Long baseCost) {
        Long milesToCheck = customerMiles + flightMiles;
        logger.warning("milesToCheck: " + milesToCheck);

        List<Integer> intIds = bs.getFlightRewardMapping();
        // for every id (miles) check if miles of customer are in that status level
        // if yes, get reductionPercentage for that status
        int lastId = 0;
        for (Integer id : intIds) {
            if (milesToCheck < id) {
                // return baseCost multiplied by reductionPercentage
                return new PriceResponse(adjustFlightPrice(id, baseCost));
            }
            // this happens only when id is not smaller than the miles to check i.e. is the highest id
            lastId = id;
        }
        return new PriceResponse(adjustFlightPrice(lastId, baseCost));
    }

    // takes id (miles) and a base price to calculate new price based on status (its mapped priceReduction)
    private long adjustFlightPrice(Integer miles, Long baseCost) {
        float reductionPercentage = 0;
        JSONObject jsonObject = bs.getFlightRewardLevel(miles);
        reductionPercentage = jsonObject.getFloat("reduction");

        logger.warning("Found reduction for status " + jsonObject.getString("status") + " is " + reductionPercentage + "%.");

        float result = ((100 - reductionPercentage) / 100) * (float) baseCost;
        return (long) result;
    }

    // takes id (loyalty points) and a base price to calculate new price based on status (its mapped priceReduction)
    private long adjustCarPrice(Integer loyaltyPoints, Long baseCost) {
        long priceReduction = 0;
        JSONObject jsonObject = bs.getCarRewardLevel(loyaltyPoints);
        priceReduction = jsonObject.getLong("reduction");

        logger.warning("Found reduction for status " + jsonObject.getString("status") + " is " + priceReduction + "€.");

        long result = baseCost - priceReduction;
        return Math.max(result, MINIMUM_CAR_PRICE);
    }

}
