package com.acmeair.web;

public class CustomerMilesResponse {

    private Long miles;
    private Long loyaltyPoints;

    public CustomerMilesResponse() {
    }

    public CustomerMilesResponse(Long miles, Long loyaltyPoints) {
        this.setMiles(miles);
        this.setLoyaltyPoints(loyaltyPoints);
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
}
