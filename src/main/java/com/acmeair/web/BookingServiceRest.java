/*******************************************************************************
 * Copyright (c) 2013, 2023 IBM Corp.
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

package com.acmeair.web;


import com.acmeair.client.CarClient;
import com.acmeair.mongo.MongoSessionCoordinator;
import com.acmeair.service.BookingService;
import com.acmeair.service.KeyGenerator;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonReaderFactory;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.io.StringReader;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

@Path("/")
@ApplicationScoped
public class BookingServiceRest {

    @Inject
    BookingService bs;

    @Inject
    JsonWebToken jwt;

    @Inject
    RewardTracker rewardTracker;

    @Inject
    @ConfigProperty(name = "TARGET_BOOKINGS_FOR_AUDIT", defaultValue = "4000")
    Integer TARGET_BOOKINGS_FOR_AUDIT;

    @Inject
    @ConfigProperty(name = "TOLERANCE_FOR_AUDIT", defaultValue = "200")
    Integer TOLERANCE_FOR_AUDIT;

    @Inject
    @RestClient
    private CarClient carClient;

    @Inject
    private MongoSessionCoordinator mongoSessionCoordinator;
    @Inject
    KeyGenerator keyGenerator;


    private static final JsonReaderFactory factory = Json.createReaderFactory(null);
    private static final Logger logger = Logger.getLogger(BookingServiceRest.class.getName());

    /**
     * Book flights.
     */
    @POST
    @Consumes({"application/x-www-form-urlencoded"})
    @Path("/bookflights")
    @Produces("text/plain")
    @Timed(name = "com.acmeair.web.BookingServiceRest.bookFlights", tags = "app=bookingservice-java")
    @RolesAllowed({"user"})
    public /* BookingInfo */ Response bookFlights(@FormParam("userid") String userid,
                                                  @FormParam("toFlightId") String toFlightId,
                                                  @FormParam("toFlightSegId") String toFlightSegId,
                                                  @FormParam("retFlightId") String retFlightId,
                                                  @FormParam("retFlightSegId") String retFlightSegId,
                                                  @FormParam("oneWayFlight") boolean oneWay) {
        return bookFlightsAndCar(userid, toFlightId, toFlightSegId, retFlightId, retFlightSegId, oneWay, null);
    }

    /**
     * Get bookins for a customer.
     */
    @GET
    @Path("/byuser/{user}")
    @Produces("text/plain")
    @Timed(name = "com.acmeair.web.bookFlights.BookingServiceRest.getBookingsByUser", tags = "app=bookingservice-java")
    @RolesAllowed({"user"})
    public Response getBookingsByUser(@PathParam("user") String userid) {

        try {
            // make sure the user isn't trying to bookflights for someone else
            if (!userid.equals(jwt.getSubject())) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            return Response.ok(bs.getBookingsByUser(userid).toString()).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Cancel bookings.
     */
    @POST
    @Consumes({"application/x-www-form-urlencoded"})
    @Path("/cancelbooking")
    @Produces("text/plain")
    @Timed(name = "com.acmeair.web.bookFlights.BookingServiceRest.cancelBookingsByNumber", tags = "app=bookingservice-java")
    @RolesAllowed({"user"})
    public Response cancelBookingsByNumber(@FormParam("number") String number,
                                           @FormParam("userid") String userid) {
        try {
            // make sure the user isn't trying to bookflights for someone else
            if (!userid.equals(jwt.getSubject())) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            String transactionId = keyGenerator.generate().toString();
            mongoSessionCoordinator.initTransaction(transactionId);

            JsonObject booking;

            try {
                JsonReader jsonReader = factory.createReader(new StringReader(bs
                        .getBooking(userid, number)));
                booking = jsonReader.readObject();
                jsonReader.close();

                bs.cancelBooking(userid, number);
            } catch (RuntimeException npe) {
                // Booking has already been deleted...
                return Response.ok("booking " + number + " deleted.").build();
            }

            boolean isOneWay = booking.getString("retFlightId").equals("NONE - ONE WAY FLIGHT");
            //TODO: check if needs fixing

            if (booking.getString("carBooked").equalsIgnoreCase("none")) {
                rewardTracker.updateRewardMiles(userid, booking.getString("flightId"),
                        booking.getString("retFlightId"), false, null, isOneWay, transactionId);
            } else {
                rewardTracker.updateRewardMiles(userid, booking.getString("flightId"),
                        booking.getString("retFlightId"), false, booking.getString("carBooked"), isOneWay, transactionId);
            }

            return Response.ok("booking " + number + " deleted.").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("/status")
    public Response status() {
        return Response.ok("OK").build();
    }

    @GET
    @Path("/rewards/customerfailures")
    public Response customerFailures() {
        return Response.ok(rewardTracker.getCustomerFailures()).build();
    }

    @GET
    @Path("/rewards/flightfailures")
    public Response flightFailures() {
        return Response.ok(rewardTracker.getFlightFailures()).build();
    }

    @GET
    @Path("/rewards/customersuccesses")
    public Response customerSucceses() {
        return Response.ok(rewardTracker.getCustomerSuccesses()).build();
    }

    @GET
    @Path("/rewards/flightsuccesses")
    public Response flightSuccesseses() {
        return Response.ok(rewardTracker.getFlightSucesses()).build();
    }

    @GET
    @Path("/audit")
    public Response audit() {

        int minBookingCount = TARGET_BOOKINGS_FOR_AUDIT - TOLERANCE_FOR_AUDIT;
        int maxBookingCount = TARGET_BOOKINGS_FOR_AUDIT + TOLERANCE_FOR_AUDIT;

        if (rewardTracker.getCustomerFailures() == 0 && rewardTracker.getFlightFailures() == 0 &&
                rewardTracker.getCustomerSuccesses() > 0 && rewardTracker.getFlightSucesses() > 0 &&
                bs.count() > minBookingCount && bs.count() < maxBookingCount) {
            return Response.ok("pass").build();
        }

        return Response.ok("fail").build();
    }

    //USER ADDED CODE:

    /**
     * Book flights.
     */
    @POST
    @Consumes({"application/x-www-form-urlencoded"})
    @Path("/bookflightsandcar")
    @Produces("text/plain")
    public Response bookFlightsAndCar(@QueryParam("userid") String userid,
                                      @QueryParam("toFlightId") String toFlightId,
                                      @QueryParam("toFlightSegId") String toFlightSegId,
                                      @QueryParam("retFlightId") String retFlightId,
                                      @QueryParam("retFlightSegId") String retFlightSegId,
                                      @QueryParam("oneWayFlight") boolean oneWay,
                                      @QueryParam("carname") String carName) {
        try {

            // make sure the user isn't trying to bookflights for someone else
//      if (!userid.equals(jwt.getSubject())) {
//        return Response.status(Response.Status.FORBIDDEN).build();
//      }

            PricesWithSessionIdDto newPrices;
            String bookingId;
            Long totalPrice;

            String transactionId = keyGenerator.generate().toString();
            mongoSessionCoordinator.initTransaction(transactionId);

            //check if one way flight
            boolean carBooked = Objects.nonNull(carName) && !carName.equals("null") && !carName.trim().isEmpty();
            if (!oneWay) {
                logger.warning("Booking is not one way.");
                newPrices = rewardTracker.updateRewardMiles (userid, toFlightId, retFlightId, true, carName, false, transactionId);

                if (carBooked) {
                    logger.warning("Booking includes car.");
                    totalPrice = newPrices.getFlightPrice() + newPrices.getCarPrice();
                    bookingId = bs.bookFlightWithCar(userid, toFlightSegId, toFlightId, retFlightId, carName,
                            // totalPrice
                            totalPrice.toString(),
                            // newFlightPrice
                            newPrices.getFlightPrice().toString(),
                            // newCarPrice (0 if no car)
                            newPrices.getCarPrice().toString());
                } else {
                    logger.warning("Booking includes no car.");
                    bookingId = bs.bookFlight(userid, toFlightSegId, toFlightId, retFlightId, newPrices.getFlightPrice().toString());
                }
                //one way flight
            } else {
                logger.warning("Booking is one way.");
                newPrices = rewardTracker.updateRewardMiles(userid, toFlightId,null, true, carName, true, transactionId);

                if (carBooked) {
                    logger.warning("Booking includes car.");
                    totalPrice = newPrices.getFlightPrice() + newPrices.getCarPrice();
                    bookingId = bs.bookFlightWithCar(userid, toFlightSegId, toFlightId, "NONE - ONE WAY FLIGHT", carName,
                            // totalPrice
                            totalPrice.toString(),
                            // newFlightPrice
                            newPrices.getFlightPrice().toString(),
                            // newCarPrice (0 if no car)
                            newPrices.getCarPrice().toString());
                } else {
                    logger.warning("Booking includes no car.");
                    bookingId = bs.bookFlight(userid, toFlightSegId, toFlightId, "NONE - ONE WAY FLIGHT", newPrices.getFlightPrice().toString());
                }
            }

            String bookingInfo = "{\"oneWay\":\"" + oneWay
                    + "\",\"price\":\"" + (newPrices.getFlightPrice() + newPrices.getCarPrice())
                    + "\",\"flightPrice\":\"" + newPrices.getFlightPrice()
                    + "\",\"carPrice\":\"" + newPrices.getCarPrice()
                    + "\",\"bookingId\":\"" + bookingId
                    + "\",\"carBooked\":\"" + (Objects.nonNull(carName) ? carName : "NONE")
                    + "\"}";

            return Response.ok(bookingInfo).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("/db/reload")
    public Response reloadDb() {
        try {
            bs.purgeDb();
            bs.loadDbs();
            return Response.ok("(Re)Loaded booking database").build();
        } catch (Exception e) {
            logger.warning(e.toString());
            return Response.serverError().build();
        }
    }

    @GET
    @Path("/db/purge")
    public Response purgeDb() {
        bs.purgeDb();
        return Response.ok("Purged booking database").build();
    }


    @GET
    @Path("/customerreward/{id}")
    @Produces("application/json")
    public CustomerMilesResponse getCustomerRewards(@PathParam("id") String customerId) {
        return bs.getCurrentMilesAndPoints(customerId);
    }
}
