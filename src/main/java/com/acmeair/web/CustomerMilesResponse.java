package com.acmeair.web;

public class CustomerMilesResponse {

    private Long miles;
    private Long loyaltyPoints;
    private String mongoSessionId;


    public CustomerMilesResponse() {
    }

    public CustomerMilesResponse(Long miles, Long loyaltyPoints) {
        this.setMiles(miles);
        this.setLoyaltyPoints(loyaltyPoints);
    }

    public CustomerMilesResponse(Long miles, Long loyaltyPoints, String mongoSessionId) {
        this.setMiles(miles);
        this.setLoyaltyPoints(loyaltyPoints);
        this.setMongoSessionId(mongoSessionId);
    }

    public Long getMiles() {
        return miles;
    }

    public void setMiles(Long miles) {
        this.miles = miles;
    }

    public Long getLoyaltyPoints() {
        return loyaltyPoints;
    }

    public void setLoyaltyPoints(Long loyaltyPoints) {
        this.loyaltyPoints = loyaltyPoints;
    }

    public String getMongoSessionId() {
        return mongoSessionId;
    }

    public void setMongoSessionId(String mongoSessionId) {
        this.mongoSessionId = mongoSessionId;
    }
}
