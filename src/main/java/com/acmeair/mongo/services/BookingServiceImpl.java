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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.mongodb.client.model.Filters.eq;

@ApplicationScoped
public class BookingServiceImpl implements BookingService {

  private static final  Logger logger = Logger.getLogger(BookingService.class.getName());

  private MongoCollection<Document> bookingCollection;

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
    bookingCollection = database.getCollection("booking");
  }
  
  /**
   * Book Flight.
   */
  public String bookFlight(String customerId, String flightId) {
    try {

      String bookingId = keyGenerator.generate().toString();

      Document bookingDoc = new Document("_id", bookingId)
          .append("customerId", customerId)
          .append("flightId", flightId)
          .append("dateOfBooking", new Date());

        bookingCollection.insertOne(bookingDoc);

      return bookingId;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String bookFlight(String customerId, String flightSegmentId, String flightId) {
    if (flightSegmentId == null) {
      return bookFlight(customerId, flightId);
    } else {

      try {

        String bookingId = keyGenerator.generate().toString();

        Document bookingDoc = new Document("_id", bookingId).append("customerId", customerId)
            .append("flightId", flightId).append("dateOfBooking", new Date())
            .append("flightSegmentId", flightSegmentId);
        
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
/*
USER ADDED CODE STARTS HERE
 */

  /**
   * Method to search a Bloat by ID. Calls BloatService via REST and expects a complete JSON object as response.
   * @param id of the Bloat to look up
   * @return returns the Bloat as string
   * @throws IOException
   */
  @Override
  public String getBloatById(String id) throws IOException {
    //REST call
    URL url = new URL("http://acmeair-bloatservice-java:9080/bloat/byid/" + id);
    HttpURLConnection con = (HttpURLConnection) url.openConnection();
    con.setRequestMethod("GET");

    // atm logging works only with "warning"
    // TODO find out how to define log level
    logger.warning("bloat request to:" + url + " returned response code : " + con.getResponseCode());

    //write http response to string buffer
    BufferedReader in = new BufferedReader(
            new InputStreamReader(con.getInputStream()));
    String inputLine;
    StringBuffer content = new StringBuffer();
    while ((inputLine = in.readLine()) != null) {
      content.append(inputLine);
    }
    in.close();

    return content.toString();
  }

  /**
   * Method to write the name and ID of a Bloat to booking-db. Name is taken from passed JSON object.
   * Instead of taking the ID from the JSON object, this method generates a new ID for the Bloat. Bloat is then written to
   * booking-db in JSON format.
   * @param bloat JSON object in string format.
   * @return returns the newly generated ID of the Bloat.
   * @throws JsonProcessingException
   */
  public String writeBloatName(String bloat) throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode jsonNode = objectMapper.readTree(bloat);

    String bloatName = jsonNode.get("bloatName").asText();

    logger.warning("got bloatname: " + bloatName);
    String id = java.util.UUID.randomUUID().toString();
    try {
      Document bloatDoc = new Document("_id", id)
              .append("bloatName", bloatName);
      bookingCollection.insertOne(bloatDoc);

      return id;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
