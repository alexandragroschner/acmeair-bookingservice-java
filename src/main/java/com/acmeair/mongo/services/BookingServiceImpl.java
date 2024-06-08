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
import com.mongodb.MongoClient;
import com.mongodb.client.ClientSession;
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

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.mongodb.client.model.Filters.eq;

@ApplicationScoped
public class BookingServiceImpl implements BookingService {

  private static final  Logger logger = Logger.getLogger(BookingService.class.getName());
  private MongoCollection<Document> bookingCollection;
  MongoDatabase database;
  Map<String, ClientSession> sessionMap = new HashMap<>();

  @Inject
  MongoClient mongoClient;

  @Inject
  KeyGenerator keyGenerator;

  @Inject
  Tracer tracer;

  @Inject
  Span activeSpan;

  @Inject
  @ConfigProperty(name = "TRACE_EXTRA_SPAN", defaultValue = "true")
  boolean TRACE_EXTRA_SPAN;

  @PostConstruct
  public void initialization() {
    logger.warning("Mongo Client Options: " + mongoClient.getMongoClientOptions().toString());
    database = mongoClient.getDatabase("acmeair-bookingdb");
    bookingCollection = database.getCollection("booking");
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

  //Returns booking ID (ids.get(0)) and mongo session ID (ids.get(1))
  @Override
  public List<String> bookFlightWithCarPrep(String customerId, String flightSegmentId, String flightId, String retFlightId,
                                            String carName, String totalPrice, String flightPrice, String carPrice) {
    List<String> ids = new ArrayList<>();
    try {
      String bookingId = keyGenerator.generate().toString();
      ids.add(bookingId);

      final ClientSession clientSession = mongoClient.startSession();
      String sessionId = keyGenerator.generate().toString();
      ids.add(sessionId);

      clientSession.startTransaction();
      Document bookingDoc = new Document("_id", bookingId)
              .append("customerId", customerId)
              .append("flightId", flightId)
              .append("retFlightId", retFlightId)
              .append("carBooked", carName)
              .append("dateOfBooking", new Date())
              .append("flightPrice", flightPrice)
              .append("carPrice", carPrice)
              .append("totalPrice", totalPrice);

      bookingCollection.insertOne(clientSession, bookingDoc);
      sessionMap.put(sessionId, clientSession);

      return ids;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

  }

  @Override
  public List<String> bookFlightPrep(String customerId, String flightSegmentId, String flightId,
                                     String retFlightId, String price) {
    List<String> ids = new ArrayList<>();
    try {
      String bookingId = keyGenerator.generate().toString();
      ids.add(bookingId);

      final ClientSession clientSession = mongoClient.startSession();
      String sessionId = keyGenerator.generate().toString();
      ids.add(sessionId);

      clientSession.startTransaction();
      Document bookingDoc = new Document("_id", bookingId)
              .append("customerId", customerId)
              .append("flightId", flightId)
              .append("retFlightId", retFlightId)
              .append("carBooked", "NONE")
              .append("dateOfBooking", new Date())
              .append("flightPrice", price)
              .append("carPrice", "0")
              .append("totalPrice", price);

      bookingCollection.insertOne(clientSession, bookingDoc);
      sessionMap.put(sessionId, clientSession);

      return ids;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String cancelBookingPrep(String user, String bookingId) {
    if (logger.isLoggable(Level.FINE)) {
      logger.fine("cancelBooking _id : " + bookingId);
    }
    try {
      final ClientSession clientSession = mongoClient.startSession();
      String sessionId = keyGenerator.generate().toString();

      clientSession.startTransaction();
      bookingCollection.deleteMany(clientSession, eq("_id", bookingId));
      sessionMap.put(sessionId, clientSession);

      return sessionId;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void commitMongoTransaction(String mongoSessionId) {
    final ClientSession session = sessionMap.get(mongoSessionId);
    session.commitTransaction();
    session.close();
    sessionMap.remove(mongoSessionId);
  }

  @Override
  public void abortMongoTransaction(String mongoSessionId) {
    final ClientSession session = sessionMap.get(mongoSessionId);
    session.abortTransaction();
    session.close();
    sessionMap.remove(mongoSessionId);
  }

}
