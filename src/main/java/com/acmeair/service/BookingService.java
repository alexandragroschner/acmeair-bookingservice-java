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

  //Returns booking ID (ids.get(0)) and mongo session ID (ids.get(1))
  List<String> bookFlightWithCarPrep(String customerId, String flightSegmentId, String flightId, String retFlightId,
                                     String carName, String totalPrice, String flightPrice, String carPrice);

  List<String> bookFlightPrep(String customerId, String flightSegmentId, String flightId,
                              String retFlightId, String price);

  String cancelBookingPrep(String user, String bookingId);

  void commitMongoTransaction(String mongoSessionId);

  void abortMongoTransaction(String mongoSessionId);
}