package com.acmeair.client.responses;

public class CarResponse {

    private String id;
    private String carName;
    private int baseCost;
    private Long loyaltyPoints;

    // this needs to use setter methods instead of "this.id = id;"
    // probably due to reflection API
    public CarResponse(String id, String carName, int baseCost, Long loyaltyPoints) {
        this.setId(id);
        this.setCarName(carName);
        this.setBaseCost(baseCost);
        this.setLoyaltyPoints(loyaltyPoints);
    }

    public CarResponse() {
        //CDI
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setCarName(String carName) {
        this.carName = carName;
    }

    public void setBaseCost(int baseCost) {
        this.baseCost = baseCost;
    }

    public String getId() {
        return id;
    }

    public String getCarName() {
        return carName;
    }

    public int getBaseCost() {
        return baseCost;
    }

    public Long getLoyaltyPoints() {
        return loyaltyPoints;
    }

    public void setLoyaltyPoints(Long loyaltyPoints) {
        this.loyaltyPoints = loyaltyPoints;
    }
}
