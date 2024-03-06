/*******************************************************************************
* Copyright (c) 2017 IBM Corp.
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

package com.acmeair.mongo.services;

import com.acmeair.service.BookingService;
import com.acmeair.service.KeyGenerator;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.mongodb.client.model.Filters.eq;

@ApplicationScoped
public class BookingServiceImpl implements BookingService {

  private static final  Logger logger = Logger.getLogger(BookingService.class.getName());

  private MongoCollection<Document> bookingCollection;
  private MongoCollection<Document> rewardFlightCollection;
  private MongoCollection<Document> rewardCarCollection;

  @Inject
  KeyGenerator keyGenerator;

  @Inject
  MongoDatabase database;

  @Inject
  Tracer tracer;

  @Inject
  Span activeSpan;

  @Inject
  @ConfigProperty(name = "TRACE_EXTRA_SPAN", defaultValue = "true")
  boolean TRACE_EXTRA_SPAN;

  @PostConstruct
  public void initialization() {
    logger.warning("Initializing BookingServiceImpl. Getting collections...");
    bookingCollection = database.getCollection("booking");
    rewardFlightCollection = database.getCollection("rewardFlight");
    rewardCarCollection = database.getCollection("rewardCar");
    try {
      loadDbs();
    } catch (Exception e) {
      logger.warning("Error in initialization of RewardServiceImpl" + e);
    }
  }
  
  /**
   * Book Flight.
   */
  public String bookFlight(String customerId, String flightId, String retFlightId, String price) {
    try {

      String bookingId = keyGenerator.generate().toString();

      Document bookingDoc = new Document("_id", bookingId)
          .append("customerId", customerId)
          .append("flightId", flightId)
          .append("retFlightId", retFlightId)
          .append("carBooked", "NONE")
          .append("dateOfBooking", new Date())
          .append("flightPrice", price)
          .append("carPrice", 0)
          .append("totalPrice", price);

        bookingCollection.insertOne(bookingDoc);

      return bookingId;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String bookFlight(String customerId, String flightSegmentId, String flightId,
                           String retFlightId, String price) {
    if (flightSegmentId == null) {
      return bookFlight(customerId, flightId, retFlightId, price);
    } else {

      try {

        String bookingId = keyGenerator.generate().toString();

        Document bookingDoc = new Document("_id", bookingId)
                .append("customerId", customerId)
                .append("flightId", flightId)
                .append("retFlightId", retFlightId)
                .append("carBooked", "NONE")
                .append("dateOfBooking", new Date())
                .append("flightPrice", price)
                .append("carPrice", "0")
                .append("totalPrice", price);
        
        if (TRACE_EXTRA_SPAN) {
          Span childSpan = tracer.spanBuilder("Created bookFlight Span")
            .setParent(Context.current().with(activeSpan))
            .startSpan();

          childSpan.setAttribute("Created", true);
          bookingCollection.insertOne(bookingDoc);
          childSpan.end();
        } else {
          bookingCollection.insertOne(bookingDoc);
        }

        return bookingId;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

    }
  }

  @Override
  public String getBooking(String user, String bookingId) {
    try {
      return bookingCollection.find(eq("_id", bookingId)).first().toJson();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public List<String> getBookingsByUser(String user) {
    List<String> bookings = new ArrayList<String>();
    if (logger.isLoggable(Level.FINE)) {
      logger.fine("getBookingsByUser : " + user);
    }
    try (MongoCursor<Document> cursor = bookingCollection.find(eq("customerId", user)).iterator()) {

      while (cursor.hasNext()) {
        Document tempBookings = cursor.next();
        Date dateOfBooking = (Date) tempBookings.get("dateOfBooking");
        tempBookings.remove("dateOfBooking");
        tempBookings.append("dateOfBooking", dateOfBooking.toString());

        if (logger.isLoggable(Level.FINE)) {
          logger.fine("getBookingsByUser cursor data : " + tempBookings.toJson());
        }
        bookings.add(tempBookings.toJson());
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return bookings;
  }

  @Override
  public void cancelBooking(String user, String bookingId) {
    if (logger.isLoggable(Level.FINE)) {
      logger.fine("cancelBooking _id : " + bookingId);
    }
    try {
      bookingCollection.deleteMany(eq("_id", bookingId));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Long count() {
    return bookingCollection.countDocuments();
  }

  @Override
  public void dropBookings() {
    bookingCollection.deleteMany(new Document());
  }

  @Override
  public String getServiceType() {
    return "mongo";
  }
  
  @Override
  public boolean isConnected() {
    return (bookingCollection.countDocuments() >= 0);
  }

  //USER ADDED CODE
  @Override
  public String bookFlightWithCar(String customerId, String flightSegmentId, String flightId, String retFlightId,
                                  String carName, String totalPrice, String flightPrice, String carPrice) {
    try {

      String bookingId = keyGenerator.generate().toString();

      Document bookingDoc = new Document("_id", bookingId)
              .append("customerId", customerId)
              .append("flightId", flightId)
              .append("retFlightId", retFlightId)
              .append("carBooked", carName)
              .append("dateOfBooking", new Date())
              .append("flightPrice", flightPrice)
              .append("carPrice", carPrice)
              .append("totalPrice", totalPrice);

      if (TRACE_EXTRA_SPAN) {
        Span childSpan = tracer.spanBuilder("Created bookFlight Span")
                .setParent(Context.current().with(activeSpan))
                .startSpan();

        childSpan.setAttribute("Created", true);
        bookingCollection.insertOne(bookingDoc);
        childSpan.end();
      } else {
        bookingCollection.insertOne(bookingDoc);
      }

      return bookingId;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

  }

  @Override
  public void loadDbs() throws IOException {
    loadDb(rewardFlightCollection, "/META-INF/milesstatusmapping.csv");
    loadDb(rewardCarCollection, "/META-INF/loyaltypointsstatusmapping.csv");
  }

  @Override
  public void loadDb(MongoCollection<Document> collection, String resource) throws IOException {

    if (collection.countDocuments() != 0) {
      logger.warning("Loading booking db aborted. Database is not empty!");
      return;
    }

    InputStream csvInputStream = BookingServiceImpl.class.getResourceAsStream(resource);

    assert csvInputStream != null;
    LineNumberReader lnr = new LineNumberReader(new InputStreamReader(csvInputStream));

    while (true) {
      String line = lnr.readLine();
      // end reading lines when EOF
      if (line == null || line.trim().isEmpty()) {
        break;
      }
      StringTokenizer st = new StringTokenizer(line, ",");
      ArrayList<String> lineAsStringArray = new ArrayList<>();

      // adds value of every column of current line to array as string
      while (st.hasMoreTokens()) {
        lineAsStringArray.add(st.nextToken());
      }
      logger.warning("Inserting values for status: " + lineAsStringArray.get(1));

      collection.insertOne(new Document("_id", lineAsStringArray.get(0))
              .append("status", lineAsStringArray.get(1))
              .append("reduction", lineAsStringArray.get(2)));
    }
  }

  @Override
  public void purgeDb() {
    for (Document d : rewardFlightCollection.find()) {
      rewardFlightCollection.deleteOne(d);
    }

    for (Document d : rewardCarCollection.find()) {
      rewardCarCollection.deleteOne(d);
    }
    logger.warning("Purged DB - Amount of documents left in reward collections: " + (rewardFlightCollection.countDocuments()
            + rewardCarCollection.countDocuments()));
  }
}
