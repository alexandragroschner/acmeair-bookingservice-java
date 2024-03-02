package com.acmeair.web;


import com.acmeair.client.*;
import com.acmeair.service.BookingService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

@ApplicationScoped
public class RewardTracker {

    @Inject
    BookingService bs;

    @Inject
    @RestClient
    private CustomerClient customerClient;

    @Inject
    @RestClient
    private FlightClient flightClient;

    @Inject
    @RestClient
    private RewardClient rewardClient;

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

    public List<Long> updateRewardMiles(String userid, String flightSegId, String flightId,
                                        String retFlightSegId, String retFlightId, boolean add,
                                        String carName, boolean isOneWay) {

        List<Long> updatedPrices = new ArrayList<>();

        CostAndMilesResponse costAndMiles = flightClient.getCostAndMiles(flightId);
        CostAndMilesResponse retCostAndMiles;
        if (isOneWay) {
            // make an empty return miles response if no return flight
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
            logger.warning("Calling car service for car: " + carName);
            if (carToBook == null) {
                logger.warning("COULD NOT FIND REQUESTED CAR");
                return null;
            }
        }

        flightSuccesses.incrementAndGet();
        //subtract miles if booking is cancelled
        if (!add) {
            costAndMiles.setMiles((costAndMiles.getMiles()) * -1);
        }

        // TODO: implement method to get current miles instead of total miles (without hack)
        //HACK: pass 0 miles to customerClient.updateCustomerTotalMiles();
        CustomerMilesResponse currentMilesAndLoyalty = customerClient.updateCustomerTotalMiles(userid, 0L, 0L);
        logger.warning("Current miles: " + currentMilesAndLoyalty.getMiles());
        logger.warning("Current loyalty: " + currentMilesAndLoyalty.getLoyaltyPoints());
        Long totalFlightMiles = costAndMiles.getMiles() + retCostAndMiles.getMiles();
        Long totalFlightPrice = costAndMiles.getCost() + retCostAndMiles.getCost();

        // pass flight miles, current miles and cost to reward service
        PriceResponse newFlightPrice = rewardClient.getNewPrice(totalFlightMiles.toString(), currentMilesAndLoyalty.getMiles().toString(), totalFlightPrice.toString());
        updatedPrices.add(newFlightPrice.getPrice());
        //get new car price
        Long loyaltyPoints = 0L;
        if (Objects.nonNull(carToBook)) {
            PriceResponse newCarPrice = rewardClient.getNewCarPrice(carToBook.getLoyaltyPoints().toString(),
                    currentMilesAndLoyalty.getLoyaltyPoints().toString(), String.valueOf(carToBook.getBaseCost()));

            logger.warning("new car price is " + newCarPrice.getPrice());
            loyaltyPoints = carToBook.getLoyaltyPoints();
            updatedPrices.add(newCarPrice.getPrice());
        } else {
            // add 0 as car price if no car is booked
            logger.warning("CAR NULL - ADDING NO CAR PRICE");
            updatedPrices.add(0L);
        }

        logger.warning("new flight price is " + newFlightPrice.getPrice());

        CustomerMilesResponse updatedMilesAndLoyalty = customerClient.updateCustomerTotalMiles(userid, totalFlightMiles, loyaltyPoints);
        logger.warning("Updated miles: " + updatedMilesAndLoyalty.getMiles());
        logger.warning("Updated loyalty: " + updatedMilesAndLoyalty.getLoyaltyPoints());

        // Both calls succeeded!
        customerSuccesses.incrementAndGet();
        logger.warning("RETURNING UPDATED PRICES");
        return updatedPrices;
    }
}
