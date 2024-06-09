package com.acmeair.web;

public class PricesWithSessionIdDto {
    private Long flightPrice;
    private Long carPrice;
    private String mongoSessionId;

    public PricesWithSessionIdDto() {
    }

    public PricesWithSessionIdDto(Long flightPrice, Long carPrice, String mongoSessionId) {
        this.setFlightPrice(flightPrice);
        this.setCarPrice(carPrice);
        this.setMongoSessionId(mongoSessionId);
    }
    public Long getFlightPrice() {
        return flightPrice;
    }

    public void setFlightPrice(Long flightPrice) {
        this.flightPrice = flightPrice;
    }

    public Long getCarPrice() {
        return carPrice;
    }

    public void setCarPrice(Long carPrice) {
        this.carPrice = carPrice;
    }

    public String getMongoSessionId() {
        return mongoSessionId;
    }

    public void setMongoSessionId(String mongoSessionId) {
        this.mongoSessionId = mongoSessionId;
    }
}
