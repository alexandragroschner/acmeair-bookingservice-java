/*******************************************************************************
* Copyright (c) 2018 IBM Corp.
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
package com.acmeair.client;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;

import java.util.logging.Logger;

@ApplicationScoped
public class CustomerFallbackHandler implements FallbackHandler<CustomerMilesResponse> {
  protected static Logger logger =  Logger.getLogger(CustomerFallbackHandler.class.getName());

  @Override
  public CustomerMilesResponse handle(ExecutionContext context) {
    System.out.println("Customer Call Failed - check connection to Customer Service.");
    logger.info("fallback for " + context.getMethod().getName());		
    return null;
  }
}
