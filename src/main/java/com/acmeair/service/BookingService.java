/*******************************************************************************
* Copyright (c) 2013 IBM Corp.
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

package com.acmeair.service;

import com.acmeair.client.responses.CustomerMilesResponse;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

public interface BookingService {

  String bookFlight(String customerId, String flightSegmentId, String flightId,
                    String retFlightId, String price);

  String getBooking(String user, String id);

  List<String> getBookingsByUser(String user);

  void cancelBooking(String user, String id);

  Long count();

  void dropBookings();

  String getServiceType();

  boolean isConnected();

  String bookFlightWithCar(String customerId, String flightSegmentId, String flightId, String retFlightId,
                           String carName, String totalPrice, String flightPrice, String carPrice);

  void loadDbs() throws IOException;

  void loadDb(MongoCollection<Document> collection, String resource) throws IOException;

  void purgeDb();

  CustomerMilesResponse getCurrentMilesAndPoints(String customerId);

  CustomerMilesResponse updateCustomerMilesAndPoints(String customerId, Long miles, Long loyaltyPoints);

  List<Integer> getCarRewardMapping();

  List<Integer> getFlightRewardMapping();

  JSONObject getFlightRewardLevel(Integer id);

  JSONObject getCarRewardLevel(Integer id);
}